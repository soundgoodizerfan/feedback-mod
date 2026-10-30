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
package io.github.soundgoodizerfan.feedback.process;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.ModularType;
import io.github.soundgoodizerfan.feedback.modular.ModularTypes;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The cooking ladder ({@code docs/specs/feedback_food_spec.md} 4.3-4.5): heat applied to whatever food is
 * in a vessel, and what that does to hunger, saturation and the hazards when it is eaten.
 *
 * <p>A vessel does not know what it is cooking. It calls {@link #tick} on any stack whose item is a
 * form of a material with a {@code food} block; the stack's {@link FoodState} moves, and when it is
 * done the item becomes the material's {@code cooked} form. Hunger and saturation of the done state
 * are the vanilla figures, read off that item; state scales them, and what is left of the pathogen
 * and toxin becomes {@code Hunger} and {@code Poison} when eaten, so nothing new enters the
 * player's meters.
 *
 * <p>One temperature for the whole item; its material's {@code mass} sets how fast it cooks and dries
 * through (food spec 3), so a thick cut wants low and slow.
 */
public final class Cooking {

    /** Where an item sits: its material's food block, and the item it cooks into. */
    public record Entry(Material.Food food, Tu charTemperature, Optional<Item> cooked, boolean kneadable, List<Material.Trait> traits, String material) {
        public Entry(Material.Food food, Tu charTemperature, Optional<Item> cooked) {
            this(food, charTemperature, cooked, false, List.of(), "");
        }
    }

    public enum Doneness { RAW, DONE, DRIED, CHARRED }

    private static Map<Item, Entry> serverIndex = Map.of();
    private static int serverGeneration = -1;
    private static Map<Item, Entry> clientIndex = Map.of();
    private static Map<ResourceLocation, Material> clientSource;

    private Cooking() {
    }

    public static Optional<Entry> find(Level level, ItemStack stack) {
        Map<Item, Entry> index;
        if (level.isClientSide) {
            Map<ResourceLocation, Material> source = ClientMaterials.get();
            if (source != clientSource) {
                clientIndex = index(source);
                clientSource = source;
            }
            index = clientIndex;
        } else {
            if (serverGeneration != MaterialTable.generation()) {
                serverIndex = index(MaterialTable.get().materials());
                serverGeneration = MaterialTable.generation();
            }
            index = serverIndex;
        }
        return Optional.ofNullable(index.get(stack.getItem()));
    }

    /**
     * As {@link #find}, and a dish too: a stack carrying {@link DishFigures} and a {@link FoodState} is a food whose
     * water is the dish's baked blend. Machines that cook or pot food keep {@link #find}; the ones that keep, dry and
     * eat it (ice box, rack, eating, tooltip) ask this.
     */
    public static Optional<Entry> findPerishable(Level level, ItemStack stack) {
        DishFigures figures = stack.get(FDataComponents.DISH_FIGURES.get());
        return figures != null && stack.has(FDataComponents.FOOD_STATE.get()) ? Optional.of(dishEntry(figures)) : find(level, stack);
    }

    /**
     * As {@link #find}, and a dish that is cooked whole in a vessel (a type with {@code bakes}: a pie): it heats as one food
     * on its own state, and {@link #tick} bakes it through when its cook reaches 1. Any other dish is not cookable.
     */
    public static Optional<Entry> findCookable(Level level, ItemStack stack) {
        DishFigures figures = stack.get(FDataComponents.DISH_FIGURES.get());
        if (figures == null)
            return find(level, stack);
        ModularDesign design = ModularStacks.design(stack);
        ModularType type = design == null ? null : ModularTypes.known().get(design.type());
        return type != null && type.bakes() && stack.has(FDataComponents.FOOD_STATE.get()) ? Optional.of(dishEntry(figures)) : Optional.empty();
    }

    /** The synthetic entry of a dish: no cooked form and nothing authored but the water it was baked at. */
    public static Entry dishEntry(DishFigures figures) {
        return new Entry(new Material.Food(figures.moisture(), 0f, 0f, Optional.empty(), 0f, 0f, Map.of(), Map.of(), 1f, 1f), FTuning.FOOD_CHAR_TU, Optional.empty());
    }

    private static Map<Item, Entry> index(Map<ResourceLocation, Material> materials) {
        Map<Item, Entry> out = new HashMap<>();
        for (Map.Entry<ResourceLocation, Material> me : materials.entrySet()) {
            Material material = me.getValue();
            if (material.food().isEmpty())
                continue;
            Material.Food food = material.food().get();
            Tu burns = food.charTemperature().or(material.thermal()::oxidationTemperature).orElse(FTuning.FOOD_CHAR_TU);
            Optional<Item> cooked = Optional.ofNullable(material.forms().get("cooked")).flatMap(BuiltInRegistries.ITEM::getOptional);
            Entry entry = new Entry(food, burns, cooked, material.traits().contains(Material.Trait.KNEADABLE), material.traits(), me.getKey().getPath());
            // Only the forms named in the file: a food's items already exist, nothing is generated for it.
            // Only the edible forms: a material with a food block may also have a block or tool form (honey has a block).
            material.forms().forEach((form, id) -> {
                if (form.equals("raw") || form.equals("cooked"))
                    BuiltInRegistries.ITEM.getOptional(id).ifPresent(item -> out.put(item, entry));
            });
        }
        return out;
    }

    /** The stack's state, or the fresh raw state of its material. */
    public static FoodState state(ItemStack stack, Entry entry) {
        FoodState state = stack.get(FDataComponents.FOOD_STATE.get());
        return state != null ? state
                : new FoodState(0f, entry.food().moisture(), entry.food().pathogen(), entry.food().toxin(), 0f, 0f, 0L, FTuning.AMBIENT_TU.value(), 0f, 0f);
    }

    /** The stack's cut, or whole. */
    private static FoodCut cutOf(ItemStack stack) {
        FoodCut cut = stack.get(FDataComponents.FOOD_CUT.get());
        return cut == null ? new FoodCut(0, FoodCut.EVEN) : cut;
    }

    /** How much faster than whole this stack cooks and dries; a covered dish dries slower, a sealed one not at all. */
    public static float cutHeat(ItemStack stack, Entry entry) {
        return cutHeat(stack) / entry.food().mass();
    }

    /** As {@link #cutHeat(ItemStack, Entry)} without the food's own mass. */
    public static float cutHeat(ItemStack stack) {
        DishFigures dish = stack.get(FDataComponents.DISH_FIGURES.get());
        return (1f + FTuning.CUT_HEAT_PER_LEVEL * cutOf(stack).level()) * (dish == null ? 1f : DishFigures.sealScale(dish.seal()));
    }

    /** A fresh state of this food, stamped now (quantised so stacks stamped within a minute still merge). */
    public static FoodState fresh(Entry entry, Level level) {
        FoodState s = state(ItemStack.EMPTY, entry);
        return new FoodState(s.cook(), s.moisture(), s.pathogen(), s.toxin(), 0f, 0f,
                level.getGameTime() / FTuning.FOOD_STAMP_BUCKET * FTuning.FOOD_STAMP_BUCKET, AmbientTable.at(level).value(), 0f, 0f);
    }

    /**
     * The state as it is now, spoilage advanced across the time since its stamp (the lazy clock, food
     * spec 4.2): nothing is ticked, so a food in a chest, a pocket or an unloaded chunk spoils correctly.
     * Rot runs at a rate set by the surroundings (the mean of then and {@code envTu} now; cold stops it)
     * and by how wet the food is, and raises the pathogen, then the toxin. Never mutates; the caller
     * decides whether to write it back.
     */
    public static FoodState current(ItemStack stack, Entry entry, Level level, float envTu) {
        FoodState s = state(stack, entry);
        long now = level.getGameTime();
        if (s.stamp() <= 0L || now <= s.stamp())
            return s.stamp() <= 0L ? new FoodState(s.cook(), s.moisture(), s.pathogen(), s.toxin(), s.charred(), s.rot(), now, envTu, s.smoke(), s.salt()) : s;
        float temp = Mth.clamp((s.envTu() + envTu) * 0.5f / FTuning.AMBIENT_TU.value(), 0f, 1.5f);
        float wet = Mth.clamp(s.moisture() / Math.max(0.05f, entry.food().moisture()), 0f, 1f);
        // A cut food has more surface open to the air, so it spoils faster.
        float cutRot = 1f + FTuning.CUT_ROT_PER_LEVEL * cutOf(stack).level();
        // The smoke coat wears off across the gap, so its shield is the mean of then and now; salt stays.
        long gap = now - s.stamp();
        float smoke = Math.max(0f, s.smoke() - gap * FTuning.SMOKE_DECAY_PER_TICK);
        float shield = (1f - FTuning.SMOKE_ROT_SHIELD * (s.smoke() + smoke) * 0.5f) * (1f - FTuning.SALT_ROT_SHIELD * s.salt());
        // A sealed container (a jar's lid) keeps the air out: nothing grows in it.
        DishFigures dish = stack.get(FDataComponents.DISH_FIGURES.get());
        float sealed = dish != null && dish.seal() >= 2 ? 0f : 1f;
        float rot = Math.min(1f, s.rot() + gap / (FTuning.FOOD_ROT_TICKS * entry.food().shelf()) * temp * wet * cutRot * shield * sealed);
        // Only the rot gained across the gap adds pathogen: a floor at rot * share would put back what heat killed, every tick.
        float pathogen = Math.min(1f, s.pathogen() + (rot - s.rot()) * FTuning.FOOD_ROT_PATHOGEN);
        float toxin = rot > FTuning.FOOD_ROT_TOXIC_AT
                ? Math.max(s.toxin(), (rot - FTuning.FOOD_ROT_TOXIC_AT) / (1f - FTuning.FOOD_ROT_TOXIC_AT) * FTuning.FOOD_ROT_TOXIN)
                : s.toxin();
        return new FoodState(s.cook(), s.moisture(), pathogen, toxin, s.charred(), rot, now, envTu, smoke, s.salt());
    }

    /** Bring this stack's spoilage up to now in surroundings of {@code envTu}, stamping it if it never was, and write it back. */
    public static void settle(ItemStack stack, Level level, float envTu) {
        findPerishable(level, stack).ifPresent(entry -> {
            FoodState now = current(stack, entry, level, envTu);
            stack.set(FDataComponents.FOOD_STATE.get(), now);
            apply(stack, entry, now);
        });
    }

    /** 0 fresh, 1 going off, 2 rotten. */
    public static int spoilage(FoodState state) {
        return state.rot() >= FTuning.FOOD_ROT_TOXIC_AT ? 2 : state.rot() >= FTuning.FOOD_ROT_GOING_OFF ? 1 : 0;
    }

    public static Doneness doneness(Entry entry, FoodState state) {
        if (state.charred() >= 1f)
            return Doneness.CHARRED;
        if (state.moisture() < entry.food().moisture() * FTuning.FOOD_DRIED_FRACTION)
            return Doneness.DRIED;
        return state.cook() < 1f ? Doneness.RAW : Doneness.DONE;
    }

    /**
     * One tick of {@code temperature} on this stack. Returns the stack to put back: the same one,
     * or its cooked form once it is done. Rewrites the eating figures every second and whenever the
     * item swaps, since the swap changes the base they scale from.
     */
    public static ItemStack tick(ItemStack stack, Entry entry, Tu temperature, Level level) {
        return tick(stack, entry, temperature, 0f, level);
    }

    /** As {@link #tick(ItemStack, Entry, Tu, Level)}, in a vessel that lays down {@code smoke} (its kind's figure) while the food cooks. */
    public static ItemStack tick(ItemStack stack, Entry entry, Tu temperature, float vesselSmoke, Level level) {
        float t = temperature.value();
        float ambient = AmbientTable.at(level).value();
        FoodState before = current(stack, entry, level, ambient);
        FoodCut cut = cutOf(stack);
        // Small pieces have less to heat through and more surface to dry from.
        float cutHeat = cutHeat(stack, entry);
        float cook = before.cook();
        float pathogen = before.pathogen();
        if (t > FTuning.FOOD_COOK_TU.value()) {
            float speed = Math.min(1f, (t - FTuning.FOOD_COOK_TU.value())
                    / (FTuning.FOOD_COOK_FULL_TU.value() - FTuning.FOOD_COOK_TU.value()));
            if (cook < 1f)
                cook = Math.min(1f, cook + speed * cutHeat / FTuning.FOOD_COOK_TICKS);
            pathogen *= 1f - speed * FTuning.FOOD_KILL_PER_TICK;
            // A ragged cut's thick middle stays underdone, so some of what was live in it survives the heat.
            if (cut.ragged())
                pathogen = Math.max(pathogen, Math.min(entry.food().pathogen(), FTuning.CUT_RAGGED_PATHOGEN_FLOOR));
        }
        float moisture = before.moisture();
        if (t > FTuning.FOOD_EVAPORATION_TU.value())
            moisture = Math.max(0f, moisture - (t - FTuning.FOOD_EVAPORATION_TU.value()) * FTuning.FOOD_DRYING_PER_TU_TICK * cutHeat);
        float charred = before.charred();
        if (t > entry.charTemperature().value())
            charred = Math.min(1f, charred + (t - entry.charTemperature().value()) * FTuning.FOOD_CHAR_PER_TU_TICK);

        // Smoke coats food that is cooking and not yet burning; past char it is only soot.
        float smoke = before.smoke();
        if (vesselSmoke > 0f && t > FTuning.FOOD_COOK_TU.value() && t < entry.charTemperature().value())
            smoke = Math.min(1f, smoke + vesselSmoke * FTuning.SMOKE_DEPOSIT_PER_TICK);

        FoodState after = new FoodState(cook, moisture, pathogen, before.toxin(), charred, before.rot(), level.getGameTime(), ambient, smoke, before.salt());
        boolean done = before.cook() < 1f && cook >= 1f && entry.cooked().isPresent() && stack.getItem() != entry.cooked().get();
        // A pie cooked through: baked again from its cooked parts, which writes its state and eating figures itself.
        if (before.cook() < 1f && cook >= 1f && stack.has(FDataComponents.DISH_FIGURES.get())) {
            stack.set(FDataComponents.FOOD_STATE.get(), after);
            ModularStacks.bakeThrough(stack, after);
            return stack;
        }
        ItemStack out = stack;
        if (done)
            out = stack.transmuteCopy(entry.cooked().get(), stack.getCount());
        out.set(FDataComponents.FOOD_STATE.get(), after);
        if (done || level.getGameTime() % 20 == 0 || doneness(entry, before) != doneness(entry, after))
            apply(out, entry, after);
        return out;
    }

    /** Stamp the eating figures this state gives onto the stack, over its item's default. */
    public static void apply(ItemStack stack, Entry entry, FoodState state) {
        DishFigures dish = stack.get(FDataComponents.DISH_FIGURES.get());
        if (dish != null) {
            applyDish(stack, dish, state);
            FoodNames.stamp(stack, entry, state);
            return;
        }
        FoodProperties base = stack.getItem().components().get(DataComponents.FOOD);
        if (base == null)
            return;
        float scale = switch (doneness(entry, state)) {
            case DRIED -> FTuning.FOOD_DRIED_SCALE;
            case CHARRED -> FTuning.FOOD_CHARRED_SCALE;
            default -> 1f;
        } * (1f - state.rot() * FTuning.FOOD_ROT_NUTRITION_LOSS) * kneaded(stack, entry);
        stack.set(DataComponents.FOOD, new FoodProperties(Math.round(base.nutrition() * scale), base.saturation() * scale,
                base.canAlwaysEat(), base.eatSeconds(), base.usingConvertsTo(), hazards(state.pathogen() / tolerance(state.salt(), entry.food().actives(), "pathogen"),
                        state.toxin() / tolerance(state.salt(), entry.food().actives(), "toxin"), state.charred())));
        FoodNames.stamp(stack, entry, state);
    }

    /**
     * A dish's eating figures: its baked ones, less what rot has taken and what drying has cost, with the hazards of the
     * pathogen and toxin it carries now. Char is not scaled here: the parts' own char is already in the baked figures.
     */
    private static void applyDish(ItemStack stack, DishFigures dish, FoodState state) {
        io.github.soundgoodizerfan.feedback.item.Meal meal = stack.get(FDataComponents.MEAL.get());
        Map<String, Float> actives = meal == null ? Map.of() : meal.compounds();
        float pathogenTol = tolerance(state.salt(), actives, "pathogen");
        float toxinTol = tolerance(state.salt(), actives, "toxin");
        float scale = (state.moisture() < dish.moisture() * FTuning.FOOD_DRIED_FRACTION ? FTuning.FOOD_DRIED_SCALE : 1f)
                * (1f - state.rot() * FTuning.FOOD_ROT_NUTRITION_LOSS);
        stack.set(DataComponents.FOOD, new FoodProperties(Math.max(1, Math.round(dish.hunger() * scale)), dish.saturation() * scale,
                false, dish.eatSeconds(), Optional.empty(), hazards(state.pathogen() / pathogenTol, state.toxin() / toxinTol, state.charred())));
        if (meal != null)
            stack.set(FDataComponents.MEAL.get(), new io.github.soundgoodizerfan.feedback.item.Meal(meal.dish(), state.pathogen() / pathogenTol,
                    meal.compounds(), meal.fat(), meal.fibre(), meal.ingredients()));
    }

    /** How the knead scales a dough's loaf: its class, or the unskilled floor for dough nobody kneaded; 1 for food that is not kneaded. */
    private static float kneaded(ItemStack stack, Entry entry) {
        if (!entry.kneadable())
            return 1f;
        FoodKnead knead = stack.get(FDataComponents.FOOD_KNEAD.get());
        return knead == null ? FTuning.KNEAD_UNKNEADED_SCALE : knead.scale();
    }

    /** What a food's hazard concentrations are divided by: salted food tolerates more. */
    public static float tolerance(float salt) {
        return 1f + FTuning.SALT_TOLERANCE * salt;
    }

    /** As {@link #tolerance(float)}, with the meal's perks: a purge or tolerance perk (garlic, kelp) moves it for that {@code hazard}. */
    public static float tolerance(float salt, Map<String, Float> actives, String hazard) {
        return FoodPerk.get().tolerance(salt, actives, hazard);
    }

    /** One food salted: its salt at 1 and the water the salt drew out gone. The item count is the caller's. */
    public static FoodState salted(FoodState s) {
        return new FoodState(s.cook(), Math.max(0f, s.moisture() - FTuning.SALT_DRAW), s.pathogen(), s.toxin(), s.charred(), s.rot(),
                s.stamp(), s.envTu(), s.smoke(), 1f);
    }

    /** What eating this much live organism, stable toxin and char does: Hunger, Poison and Nausea, by concentration over a tolerance. */
    public static List<FoodProperties.PossibleEffect> hazards(float pathogen, float toxin, float charred) {
        List<FoodProperties.PossibleEffect> effects = new java.util.ArrayList<>();
        if (pathogen > FTuning.FOOD_HAZARD_TOLERANCE)
            effects.add(new FoodProperties.PossibleEffect(() -> new MobEffectInstance(MobEffects.HUNGER, 600), Math.min(1f, pathogen)));
        if (toxin > FTuning.FOOD_HAZARD_TOLERANCE)
            effects.add(new FoodProperties.PossibleEffect(() -> new MobEffectInstance(MobEffects.POISON, 100), Math.min(1f, toxin)));
        if (charred >= 1f)
            effects.add(new FoodProperties.PossibleEffect(() -> new MobEffectInstance(MobEffects.CONFUSION, 200), 0.5f));
        return effects;
    }
}
