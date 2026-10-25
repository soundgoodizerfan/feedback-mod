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
package io.github.soundgoodizerfan.feedback.modular;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.process.Cooking;
import io.github.soundgoodizerfan.feedback.process.DishFigures;
import io.github.soundgoodizerfan.feedback.process.FoodCut;
import io.github.soundgoodizerfan.feedback.process.FoodState;
import io.github.soundgoodizerfan.feedback.process.Material;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.food.FoodProperties;

/**
 * What a dish is, from its parts ({@code docs/feedback_modular_dishes_spec.md} 2.3): a pure function of the
 * design and the material table, so it reads each part's {@link FoodState} off {@link Part#dims} where the
 * Tool Table put it. A dish is as good as its worst part for hazards, and a bowl can only hold so much meal.
 *
 * <p>The dish also leaves with a {@link FoodState} of its own (rot of the worst part, the rest blended by part
 * volume) that the lazy clock then ages. Wet parts wet the dry ones (migration, spec 2.3): the blend is conserved, a
 * dry part beside a wet one is soaked, and the container's seal scales how far the water travels.
 */
public final class DishBake {

    private DishBake() {
    }

    /**
     * The meal the parts make. {@code litres} is the container's volume; the rest are the figures the stack's
     * {@code food} and {@code meal} components are written from. {@code pathogen} and {@code toxin} are over the
     * salt's tolerance (what is shown); {@code state} holds them raw, as {@link Cooking#apply} divides them again.
     * {@code overflow} is the mB of food over the container's volume; {@code wetHunger} and {@code wetSaturation} are the
     * wettest part's share of the figures, which spilling takes from; {@code seal} is the container's.
     */
    public record Dish(float hunger, float saturation, float pathogen, float toxin, float charred,
                       float fat, float fibre, float litres, Map<String, Float> compounds, FoodState state,
                       float overflow, float wetHunger, float wetSaturation, int seal) {}

    /** The pot serving words, so a soup's name can say which one it was poured from. */
    public static final java.util.List<String> SERVINGS = java.util.List.of("broth", "stew", "scorched");

    /** A pot serving's figures as part dims: its baked nutrition, load, fat and fibre, its compounds as {@code c:name}. */
    public static Map<String, Float> servingDims(net.minecraft.world.item.ItemStack stack) {
        Map<String, Float> out = new HashMap<>();
        FoodProperties food = stack.has(DataComponents.FOOD) ? stack.get(DataComponents.FOOD) : stack.getItem().components().get(DataComponents.FOOD);
        io.github.soundgoodizerfan.feedback.item.Meal meal = stack.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.MEAL.get());
        if (food != null) {
            out.put(HUNGER, (float) food.nutrition());
            out.put(SATURATION, food.saturation());
            food.effects().stream().filter(e -> e.effect().getEffect().equals(net.minecraft.world.effect.MobEffects.POISON))
                    .findFirst().ifPresent(e -> out.put(TOXIN, e.probability()));
        }
        if (meal != null) {
            out.put(PATHOGEN, meal.pathogen());
            out.put("fat", meal.fat());
            out.put("fibre", meal.fibre());
            out.put(SERVING, (float) Math.max(0, SERVINGS.indexOf(meal.dish())));
            meal.compounds().forEach((name, strength) -> out.put(COMPOUND + name, strength));
        }
        return out;
    }

    /** The part dims that carry a food's state ({@link FoodState}), written by the Tool Table. */
    private static final String COOK = "cook", MOISTURE = "moisture", PATHOGEN = "pathogen", TOXIN = "toxin",
            CHARRED = "charred", ROT = "rot", SMOKE = "smoke", SALT = "salt",
            HUNGER = "hunger", SATURATION = "saturation", COMPOUND = "c:";
    /** The dim a serving part carries: which of {@link #SERVINGS} it is. A dish's name reads it. */
    public static final String SERVING = "serving";

    /** A food's state as dims on its part: the one place the key names are spelled. */
    public static Map<String, Float> dimsOf(FoodState s) {
        Map<String, Float> out = new HashMap<>();
        out.put(COOK, s.cook());
        out.put(MOISTURE, s.moisture());
        out.put(PATHOGEN, s.pathogen());
        out.put(TOXIN, s.toxin());
        out.put(CHARRED, s.charred());
        out.put(ROT, s.rot());
        out.put(SMOKE, s.smoke());
        out.put(SALT, s.salt());
        return out;
    }

    /** The state of a food that nothing has happened to: its material's own, cooked through if the item is the cooked form. */
    public static FoodState fresh(Material.Food food, String form) {
        boolean cooked = form.equals("cooked");
        return new FoodState(cooked ? 1f : 0f, food.moisture(), cooked ? 0f : food.pathogen(), food.toxin(), 0f, 0f, 0L, FTuning.AMBIENT_TU.value(), 0f, 0f);
    }

    /** A food part's cut (level, grade) and pour off its dims; absent dims are whole, and a finish nobody poured is the automation floor. */
    private static final String CUT = "cut", EVENNESS = "evenness", POUR = "pour";

    static FoodState stateOf(Part p, Material.Food food) {
        FoodState f = fresh(food, p.form());
        return new FoodState(p.dim(COOK, f.cook()), p.dim(MOISTURE, f.moisture()), p.dim(PATHOGEN, f.pathogen()),
                p.dim(TOXIN, f.toxin()), p.dim(CHARRED, f.charred()), p.dim(ROT, f.rot()), 0L, f.envTu(), p.dim(SMOKE, 0f), p.dim(SALT, 0f));
    }

    private static Optional<FoodProperties> eaten(Material m, String form) {
        ResourceLocation id = m.forms().get(form);
        return id == null ? Optional.empty()
                : BuiltInRegistries.ITEM.getOptional(id).map(i -> i.components().get(DataComponents.FOOD));
    }

    /**
     * Water across each neighbouring pair of food parts: {@code k} of the gap moves from the wetter to the drier, so the sum is
     * conserved. A part under the dry line beside one over the soggy line is soaked.
     */
    private static float[] migrate(float[] m, int n, boolean[] soaked, float k) {
        float[] out = java.util.Arrays.copyOf(m, n);
        if (k <= 0f)
            return out;
        for (int i = 0; i + 1 < n; i++) {
            int lo = m[i] < m[i + 1] ? i : i + 1, hi = i + i + 1 - lo;
            float t = k * (m[hi] - m[lo]) / 2f;
            out[lo] += t;
            out[hi] -= t;
            if (m[lo] < FTuning.DISH_DRY_MOISTURE && m[hi] > FTuning.DISH_SOGGY_MOISTURE)
                soaked[lo] = true;
        }
        return out;
    }

    /** The dish {@code design} makes, or empty with no container or no food, or a food part with no {@code food} block. */
    public static Optional<Dish> of(ModularDesign design, ModularType type, Map<ResourceLocation, Material> materials) {
        Part container = design.part(type.primary());
        if (container == null)
            return Optional.empty();
        float litres = type.roles().get(type.primary()).litres() * container.count();
        int seal = type.roles().get(type.primary()).seal();
        // Moisture migration: each food part's water in laying order, and where it ends up for its doneness and soaking.
        java.util.IdentityHashMap<Part, Integer> slot = new java.util.IdentityHashMap<>();
        float[] water = new float[design.parts().size()];
        int foods = 0;
        for (Part p : design.parts()) {
            Material m = (p == container && !container.kind().isEmpty()) ? null : materials.get(p.material());
            if (m == null)
                continue;
            if (m.traits().contains(Material.Trait.BROTH))
                water[foods] = FTuning.SERVING_MOISTURE;
            else if (m.food().isPresent())
                water[foods] = stateOf(p, m.food().get()).moisture();
            else
                continue;
            slot.put(p, foods++);
        }
        boolean[] soaked = new boolean[foods];
        float[] migrated = migrate(water, foods, soaked, FTuning.MIGRATION * DishFigures.sealScale(seal));
        float fill = 0, wetMoisture = -1, wetHunger = 0, wetSaturation = 0;
        float hunger = 0, saturation = 0, pathogen = 0, toxinMass = 0, charred = 0, fatMass = 0, fibreMass = 0, volume = 0;
        float moistureSum = 0, smokeSum = 0, saltSum = 0, salineMb = 0, rot = 0;
        Map<String, Float> compoundMass = new HashMap<>();
        for (Part p : design.parts()) {
            // A shaped container (bowl, skewer) is no food; a flatbread is both container and food, and counts as any food part.
            if (p == container && !container.kind().isEmpty())
                continue;
            Material m = materials.get(p.material());
            if (m == null)
                return Optional.empty();
            // A pot serving is a food already baked: its nutrition counts once, and the pot's water is in it.
            if (m.traits().contains(Material.Trait.BROTH)) {
                float v = FTuning.DISH_PART_MB;
                hunger += p.dim(HUNGER, 0f);
                saturation += p.dim(SATURATION, 0f);
                if (FTuning.SERVING_MOISTURE > wetMoisture) {
                    wetMoisture = FTuning.SERVING_MOISTURE;
                    wetHunger = p.dim(HUNGER, 0f);
                    wetSaturation = p.dim(SATURATION, 0f);
                }
                fill += v;
                pathogen = Math.max(pathogen, p.dim(PATHOGEN, 0f));
                toxinMass += p.dim(TOXIN, 0f) * v;
                fatMass += p.dim("fat", 0f) * v;
                fibreMass += p.dim("fibre", 0f) * v;
                p.dims().forEach((key, strength) -> {
                    if (key.startsWith(COMPOUND))
                        compoundMass.merge(key.substring(COMPOUND.length()), strength * v, Float::sum);
                });
                moistureSum += FTuning.SERVING_MOISTURE * v;
                volume += v;
                continue;
            }
            // A salt is no food but a seasoning: it feeds nothing, and it raises what the dish tolerates.
            if (m.food().isEmpty()) {
                if (!m.traits().contains(Material.Trait.SALINE))
                    return Optional.empty();
                saltSum += FTuning.DISH_PART_MB;
                salineMb += FTuning.DISH_PART_MB;
                continue;
            }
            Material.Food food = m.food().get();
            FoodState s = stateOf(p, food);
            int at = slot.get(p);
            boolean soak = soaked[at];
            // The water that has crept in decides whether it still counts as dried out; the dish keeps its own blend.
            FoodState wetted = new FoodState(s.cook(), migrated[at], s.pathogen(), s.toxin(), s.charred(), s.rot(), 0L, s.envTu(), s.smoke(), s.salt());
            int cutLevel = Math.round(p.dim(CUT, 0f));
            boolean ragged = cutLevel > 0 && Math.round(p.dim(EVENNESS, 0f)) == FoodCut.RAGGED;
            // Part.role is the role's letter; the finish is the type's after role.
            float pour = p.role().equals(type.afterRole()) ? p.dim(POUR, FTuning.POUR_AUTOMATION) : 1f;
            // The same raw-to-done interpolation the pot makes, then the ladder's scale and what rot has taken.
            FoodProperties held = eaten(m, p.form()).orElse(null);
            FoodProperties done = eaten(m, "cooked").orElse(held);
            if (held == null)
                continue;
            if (p.form().equals("cooked"))
                done = held;
            float scale = switch (Cooking.doneness(new Cooking.Entry(food, FTuning.FOOD_CHAR_TU, Optional.empty()), wetted)) {
                case DRIED -> soak ? 1f : FTuning.FOOD_DRIED_SCALE;
                case CHARRED -> FTuning.FOOD_CHARRED_SCALE;
                default -> 1f;
            } * (soak ? FTuning.SOAK_SCALE : 1f);
            float h = (held.nutrition() + (done.nutrition() - held.nutrition()) * s.cook()) * scale;
            float sat = (held.saturation() + (done.saturation() - held.saturation()) * s.cook()) * scale;
            hunger += h;
            saturation += sat;
            if (s.moisture() > wetMoisture) {
                wetMoisture = s.moisture();
                wetHunger = h;
                wetSaturation = sat;
            }
            pathogen = Math.max(pathogen, Math.max(s.pathogen(), s.rot() * FTuning.FOOD_ROT_PATHOGEN));
            // A ragged cut's thick middle stays underdone: a cooked part keeps a residue the later heat step has to clear.
            if (ragged && s.cook() >= 1f)
                pathogen = Math.max(pathogen, Math.min(food.pathogen(), FTuning.CUT_RAGGED_PATHOGEN_FLOOR));
            charred = Math.max(charred, s.charred());
            float v = FTuning.DISH_PART_MB;
            // Rot is the worst part's, and takes its share off the whole dish when it is read; the rest blend by volume.
            rot = Math.max(rot, s.rot());
            moistureSum += s.moisture() * v;
            smokeSum += s.smoke() * v;
            saltSum += s.salt() * v;
            toxinMass += s.toxin() * v;
            fatMass += food.fat() * v;
            // Small pieces give up what is in them sooner: less fibre holding it back.
            fibreMass += food.fibre() * Math.max(0f, 1f - FTuning.CUT_FIBRE_LOSS_PER_LEVEL * cutLevel) * v;
            food.compounds().forEach((name, strength) -> compoundMass.merge(name, strength * pour * v, Float::sum));
            volume += v;
            if (p != container)
                fill += v;
        }
        if (volume <= 0)
            return Optional.empty();
        // A bowl cannot hold a feast: the cap falls on hunger and saturation together, so a small bowl is a small meal.
        float cap = litres * FTuning.DISH_HUNGER_PER_LITRE;
        if (hunger > cap) {
            wetHunger *= cap / hunger;
            wetSaturation *= cap / hunger;
            saturation *= cap / hunger;
            hunger = cap;
        }
        Map<String, Float> compounds = new HashMap<>();
        float total = volume;
        compoundMass.forEach((name, mass) -> compounds.put(name, mass / total));
        float salt = saltSum / (volume + salineMb);
        float tolerance = Cooking.tolerance(salt);
        // A dish is served: cooked through, unstamped until first read (the lazy clock treats that as now).
        FoodState state = new FoodState(1f, moistureSum / volume, pathogen, toxinMass / volume, charred, rot, 0L, FTuning.AMBIENT_TU.value(), smokeSum / volume, salt);
        return Optional.of(new Dish(hunger, saturation, pathogen / tolerance, toxinMass / volume / tolerance, charred, fatMass / volume, fibreMass / volume, litres, compounds, state,
                Math.max(0f, fill - litres * 1000f), wetHunger, wetSaturation, seal));
    }

    /**
     * A dish was carried (picked up or dropped): if it is overfull it loses a share of the wettest part, until it is down to what
     * its container holds. Server side; the baked hunger and saturation fall, the toxin and pathogen load do not (the water went,
     * the load stayed). A covered container spills less, a sealed one not at all.
     */
    public static void carry(net.minecraft.world.item.ItemStack stack) {
        DishFigures f = stack.get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.DISH_FIGURES.get());
        if (f == null || !f.spilling())
            return;
        float lost = Math.min(f.room(), f.spilled() + FTuning.SPILL_SHARE * f.room() * DishFigures.sealScale(f.seal()));
        if (lost <= f.spilled())
            return;
        stack.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.DISH_FIGURES.get(),
                new DishFigures(f.hunger(), f.saturation(), f.eatSeconds(), f.moisture(), lost, f.overflow(), f.seal()));
        ModularStacks.rebake(stack);
    }
}
