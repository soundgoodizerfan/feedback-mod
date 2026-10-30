/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.machine.stockpot;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.item.Meal;
import io.github.soundgoodizerfan.feedback.process.Cooking;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/**
 * A pot that cooks a batch (food spec 5). It holds one mixture, not a list of dishes: water and
 * ingredients are summed into totals (energy, toxin, pathogen, cook progress, volume), the way a
 * real pot blends them, and it never matches a recipe. The pot applies one physical operation, heat.
 *
 * <ul>
 * <li><b>Boiling pins it.</b> While there is water the pot sits at {@link FTuning#POT_BOIL_TU} however
 * hot the fire is, and the fire only boils the water away faster. Low and slow is the safe way to
 * cook; a hard fire boils it dry, and a dry pot climbs to the fire's heat and scorches.</li>
 * <li><b>Toxin is a concentration</b> ({@code toxinMass / volume}): water and clean food dilute it, boiling
 * down concentrates it. Heat does not touch it. Pathogen is killed as the batch cooks through.</li>
 * <li><b>Servings follow volume.</b> Water carries no energy, so a thin pot has many weak servings.</li>
 * </ul>
 *
 * shortcut: nothing is caught up across an unloaded chunk (the pot just waits), and the dish name
 * is two classes (broth, stew) plus scorched; refine when a dish list is wanted.
 */
public class StockpotBlockEntity extends BlockEntity {

    private float temperature = FTuning.AMBIENT_TU.value();
    private float water, solid;
    // Sum of food mass x volume; over solid it is the mean mass of what is in, which sets how fast the batch cooks.
    private float massSum;
    private float rawNutrition, cookedNutrition, rawSaturation, cookedSaturation;
    private float toxinMass, pathogenMass, cook, charred, fatMass, fibreMass;
    private float mixed, scum, folded, stir;
    // Salt in the mixture (from salted food) as a mass; saltItems is the crust a sea-water boil-dry left, boiled the water gone since the last dry.
    private float saltMass, saltItems, boiled;
    private long stirTick, skimTick;
    private final java.util.Map<String, Float> compoundMass = new java.util.HashMap<>();
    // Mass added per food material path, so a serving can be named from what went in.
    private final java.util.Map<String, Float> ingredientMass = new java.util.HashMap<>();

    public StockpotBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.STOCKPOT.get(), pos, state);
    }

    public float volume() {
        return water + solid;
    }

    public int servings() {
        return volume() <= 0f ? 0 : Math.max(1, Math.round(volume() / FTuning.POT_SERVING_MB));
    }

    public float cook() {
        return cook;
    }

    public float charred() {
        return charred;
    }

    public float scum() {
        return scum;
    }

    public float mixed() {
        return mixed;
    }

    public float temperature() {
        return temperature;
    }

    public float water() {
        return water;
    }

    /** The player is stirring at this pace (0 to 1); it holds for half a second, then lapses. */
    public void stir(float pace) {
        stir = Mth.clamp(pace, 0f, 1f);
        stirTick = level == null ? 0L : level.getGameTime();
    }

    /** Lift scum off the surface; a skim a moment after the last does nothing. */
    public void skim() {
        long now = level == null ? 0L : level.getGameTime();
        if (now - skimTick < 5L || scum <= 0f)
            return;
        skimTick = now;
        scum = Math.max(0f, scum - FTuning.POT_SKIM_AMOUNT);
        changed();
    }

    public int items() {
        return Math.round(solid / FTuning.POT_ITEM_MB);
    }

    /** Add water, up to what the pot takes; returns whether any went in. */
    public boolean addWater(float mb) {
        if (water + mb > FTuning.POT_MAX_WATER_MB)
            return false;
        // Water is not food: it dilutes but leaves the cook progress alone.
        mix(mb, true);
        water += mb;
        changed();
        return true;
    }

    /** Add one food to the mixture. Returns whether it fit. */
    public boolean addFood(ItemStack food, Cooking.Entry entry) {
        if (items() >= FTuning.POT_MAX_ITEMS)
            return false;
        float vol = FTuning.POT_ITEM_MB;
        FoodProperties held = food.has(DataComponents.FOOD) ? food.get(DataComponents.FOOD) : food.getItem().components().get(DataComponents.FOOD);
        FoodProperties done = entry.cooked().map(i -> i.components().get(DataComponents.FOOD)).orElse(held);
        if (held == null)
            return false;
        if (done == null || food.getItem() == entry.cooked().orElse(null))
            done = held;
        var state = Cooking.current(food, entry, level, AmbientTable.at(level).value());
        float total = volume();
        cook = total + vol <= 0f ? 0f : (cook * total + state.cook() * vol) / (total + vol);
        rawNutrition += held.nutrition();
        cookedNutrition += done.nutrition();
        rawSaturation += held.saturation();
        cookedSaturation += done.saturation();
        toxinMass += state.toxin() * vol;
        saltMass += state.salt() * vol;
        // The load a raw item carries; how cooked the blend is decides how much of it is left.
        pathogenMass += Math.max(entry.food().pathogen(), state.rot() * FTuning.FOOD_ROT_PATHOGEN) * vol;
        mix(vol, false);
        fatMass += entry.food().fat() * vol;
        fibreMass += entry.food().fibre() * vol;
        entry.food().actives().forEach((name, strength) -> compoundMass.merge(name, strength * vol, Float::sum));
        if (!entry.material().isEmpty())
            ingredientMass.merge(entry.material(), vol, Float::sum);
        massSum += entry.food().mass() * vol;
        solid += vol;
        changed();
        return true;
    }

    /**
     * Shake a pinch of salt (or any seasoning with a food block) straight into the pot: it adds no volume and no hunger, only its salt
     * and its perks, as much as one salted food would. Returns whether it went in.
     */
    public boolean addPinch(Cooking.Entry entry) {
        if (volume() <= 0f)
            return false;
        saltMass += FTuning.POT_ITEM_MB;
        entry.food().actives().forEach((name, strength) -> compoundMass.merge(name, strength * FTuning.POT_ITEM_MB, Float::sum));
        changed();
        return true;
    }

    /** Something at room temperature joins the pot: the heat is shared by mass, and over water the pot cannot be hotter than boiling. */
    private void mix(float mb, boolean isWater) {
        float mass = FTuning.POT_EMPTY_MASS_MB + volume();
        temperature = (temperature * mass + AmbientTable.at(level).value() * mb) / (mass + mb);
        if (isWater || water > 0f)
            temperature = Math.min(temperature, FTuning.POT_BOIL_TU.value());
    }

    /** Ladle one serving into a meal, or empty if the pot is. */
    public ItemStack ladle() {
        int n = servings();
        if (n == 0)
            return ItemStack.EMPTY;
        float v = volume();
        float scale = 1f - charred * (1f - FTuning.FOOD_CHARRED_SCALE);
        // An unmixed pot has let its solids settle: the first ladle of several is thin and what stays behind is richer. The last takes all.
        float share = n == 1 ? 1f : (1f - FTuning.POT_SETTLE * (1f - mixed)) / n;
        float nutrition = (rawNutrition + (cookedNutrition - rawNutrition) * cook) * FTuning.POT_STEW_BONUS * scale * share;
        float saturation = (rawSaturation + (cookedSaturation - rawSaturation) * cook) * FTuning.POT_STEW_BONUS * scale * share;
        if (Math.round(nutrition) < 1 && saturation <= 0f)
            return ItemStack.EMPTY;
        // A salted pot tolerates more of both, as a salted food does.
        java.util.Map<String, Float> doses = new java.util.HashMap<>();
        compoundMass.forEach((name, mass) -> doses.put(name, mass / v));
        float toxin = toxinMass / v / Cooking.tolerance(Math.min(1f, saltMass / v), doses, "toxin");
        float pathogen = (pathogenMass * (1f - cook) / v + (scum + folded) * FTuning.POT_SCUM_HAZARD) / Cooking.tolerance(Math.min(1f, saltMass / v), doses, "pathogen");
        String dish = charred >= 0.5f ? "scorched" : solid / v < FTuning.POT_BROTH_SOLIDS ? "broth" : "stew";
        ItemStack meal = new ItemStack(FItems.MEAL.get());
        java.util.Map<String, Float> shares = new java.util.HashMap<>();
        float seen = 0f;
        for (float m : ingredientMass.values())
            seen += m;
        float whole = seen;
        if (whole > 0f)
            ingredientMass.forEach((name, mass) -> shares.put(name, mass / whole));
        meal.set(FDataComponents.MEAL.get(), new Meal(dish, pathogen, doses, fatMass / v, fibreMass / v, shares));
        meal.set(DataComponents.FOOD, new FoodProperties(Math.max(1, Math.round(nutrition)), saturation, false, 1.6f,
                Optional.of(new ItemStack(Items.BOWL)), Cooking.hazards(pathogen, toxin, charred)));
        float keep = (n - 1f) / n;
        water *= keep;
        solid *= keep;
        massSum *= keep;
        rawNutrition *= 1f - share;
        cookedNutrition *= 1f - share;
        rawSaturation *= 1f - share;
        cookedSaturation *= 1f - share;
        toxinMass *= keep;
        saltMass *= keep;
        pathogenMass *= keep;
        fatMass *= keep;
        fibreMass *= keep;
        scum *= keep;
        folded *= keep;
        compoundMass.replaceAll((k, m) -> m * keep);
        ingredientMass.replaceAll((k, m) -> m * keep);
        if (n == 1)
            empty();
        changed();
        return meal;
    }

    /** The salt a sea-water boil-dry left, whole items only; none while there is water over it. */
    public int takeSalt() {
        if (water > 0f)
            return 0;
        int n = (int) saltItems;
        saltItems -= n;
        if (n > 0)
            changed();
        return n;
    }

    public void empty() {
        water = solid = massSum = rawNutrition = cookedNutrition = rawSaturation = cookedSaturation = toxinMass = pathogenMass = saltMass = boiled = cook = charred = fatMass = fibreMass = mixed = scum = folded = 0f;
        compoundMass.clear();
        ingredientMass.clear();
        changed();
    }

    private void changed() {
        setChanged();
    }

    public void tickServer() {
        if (level == null)
            return;
        float fire = HeatSource.below(level, worldPosition).value();
        float target = water > 0f ? Math.min(fire, FTuning.POT_BOIL_TU.value()) : fire;
        target = Math.max(target, AmbientTable.at(level).value());
        temperature += (target - temperature) * FTuning.POT_RELAX_PER_TICK * FTuning.POT_EMPTY_MASS_MB / (FTuning.POT_EMPTY_MASS_MB + volume());
        if (volume() <= 0f)
            return;
        // Stirring keeps the bottom from scorching and mixes the pot; it lapses half a second after the last input.
        float stirNow = level.getGameTime() - stirTick <= 10L ? stir : 0f;
        mixed = Mth.clamp(mixed + stirNow * FTuning.POT_MIX_PER_TICK - FTuning.POT_MIX_DECAY_PER_TICK, 0f, 1f);
        if (water > 0f && fire > FTuning.POT_SCORCH_FIRE_TU.value())
            charred = Mth.clamp(charred + (fire - FTuning.POT_SCORCH_FIRE_TU.value()) * FTuning.POT_SCORCH_PER_TU_TICK * (1f - mixed), 0f, 1f);
        // Raw load cooking off rises as scum; stirring it in before it is skimmed folds it into the body for good.
        if (temperature > FTuning.FOOD_COOK_TU.value() && cook < 1f && pathogenMass > 0f) {
            float speed = Math.min(1f, (temperature - FTuning.FOOD_COOK_TU.value())
                    / (FTuning.FOOD_COOK_FULL_TU.value() - FTuning.FOOD_COOK_TU.value()));
            scum = Math.min(1f, scum + pathogenMass * (1f - cook) / volume() * speed * FTuning.POT_SCUM_YIELD);
        }
        float fold = Math.min(scum, stirNow * FTuning.POT_FOLD_PER_TICK);
        scum -= fold;
        folded += fold;
        if (water > 0f && fire > FTuning.POT_BOIL_TU.value()) {
            float gone = Math.min(water, (fire - FTuning.POT_BOIL_TU.value()) * FTuning.POT_EVAPORATION_MB_PER_TU_TICK);
            water -= gone;
            boiled += gone;
            // Sea water is the brine: only a pot by the sea leaves salt when it boils dry.
            if (water <= 0f) {
                if (level.getBiome(worldPosition).is(BiomeTags.IS_OCEAN) || level.getBiome(worldPosition).is(BiomeTags.IS_BEACH))
                    saltItems += boiled / FTuning.SALT_MB_PER_ITEM;
                boiled = 0f;
            }
        }
        if (temperature > FTuning.FOOD_COOK_TU.value() && cook < 1f) {
            float speed = Math.min(1f, (temperature - FTuning.FOOD_COOK_TU.value())
                    / (FTuning.FOOD_COOK_FULL_TU.value() - FTuning.FOOD_COOK_TU.value()));
            cook = Math.min(1f, cook + speed * (1f + FTuning.POT_STIR_COOK_BONUS * mixed) / (FTuning.FOOD_COOK_TICKS * (solid > 0f ? massSum / solid : 1f)));
        }
        if (temperature > FTuning.FOOD_CHAR_TU.value())
            charred = Mth.clamp(charred + (temperature - FTuning.FOOD_CHAR_TU.value()) * FTuning.FOOD_CHAR_PER_TU_TICK, 0f, 1f);
        if (level instanceof ServerLevel server && level.getGameTime() % 10 == 0) {
            double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 1.0, z = worldPosition.getZ() + 0.5;
            if (charred > 0.02f)
                server.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 2, 0.15, 0.05, 0.15, 0.01);
            else if (temperature >= FTuning.POT_BOIL_TU.value() - 5f && water > 0f)
                server.sendParticles(ParticleTypes.CLOUD, x, y, z, 1, 0.15, 0.05, 0.15, 0.01);
        }
        changed();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Temperature", temperature);
        tag.putFloat("Water", water);
        tag.putFloat("Solid", solid);
        tag.putFloat("MassSum", massSum);
        tag.putFloat("RawN", rawNutrition);
        tag.putFloat("CookedN", cookedNutrition);
        tag.putFloat("RawS", rawSaturation);
        tag.putFloat("CookedS", cookedSaturation);
        tag.putFloat("Toxin", toxinMass);
        tag.putFloat("SaltMass", saltMass);
        tag.putFloat("SaltItems", saltItems);
        tag.putFloat("Boiled", boiled);
        tag.putFloat("Pathogen", pathogenMass);
        tag.putFloat("Cook", cook);
        tag.putFloat("Charred", charred);
        tag.putFloat("Mixed", mixed);
        tag.putFloat("Scum", scum);
        tag.putFloat("Folded", folded);
        tag.putFloat("Fat", fatMass);
        tag.putFloat("Fibre", fibreMass);
        CompoundTag doses = new CompoundTag();
        compoundMass.forEach(doses::putFloat);
        tag.put("Compounds", doses);
        CompoundTag ing = new CompoundTag();
        ingredientMass.forEach(ing::putFloat);
        tag.put("Ingredients", ing);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : FTuning.AMBIENT_TU.value();
        water = tag.getFloat("Water");
        solid = tag.getFloat("Solid");
        massSum = tag.getFloat("MassSum");
        rawNutrition = tag.getFloat("RawN");
        cookedNutrition = tag.getFloat("CookedN");
        rawSaturation = tag.getFloat("RawS");
        cookedSaturation = tag.getFloat("CookedS");
        toxinMass = tag.getFloat("Toxin");
        saltMass = tag.getFloat("SaltMass");
        saltItems = tag.getFloat("SaltItems");
        boiled = tag.getFloat("Boiled");
        pathogenMass = tag.getFloat("Pathogen");
        cook = tag.getFloat("Cook");
        charred = tag.getFloat("Charred");
        mixed = tag.getFloat("Mixed");
        scum = tag.getFloat("Scum");
        folded = tag.getFloat("Folded");
        fatMass = tag.getFloat("Fat");
        fibreMass = tag.getFloat("Fibre");
        compoundMass.clear();
        CompoundTag doses = tag.getCompound("Compounds");
        for (String key : doses.getAllKeys())
            compoundMass.put(key, doses.getFloat(key));
        ingredientMass.clear();
        CompoundTag ing = tag.getCompound("Ingredients");
        for (String key : ing.getAllKeys())
            ingredientMass.put(key, ing.getFloat(key));
    }
}
