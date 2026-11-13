// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.prep;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign.Part;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures.Fig;
import io.github.soundgoodizerfan.feedback.process.Cooking;
import io.github.soundgoodizerfan.feedback.process.FoodState;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.type.ModularDesign;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A dish as it reads now: the baked figures plus the live state (the lazy clock's cook, water, salt, smoke, rot and warmth)
 * and the composition (volume shares by what the parts are, and the cuisine the parts vote for). Nothing here is new data: it is
 * read off the stack's design, {@link FoodState} and material kinds, by property and never by item. The judge ({@code village/CommissionJudge})
 * and the tasting spoon both read it, so a long haul that rots or cools a dish moves its profile away from the baked one.
 */
public final class DishProfile {

    private DishProfile() {
    }

    /** One tasting line: the axis and the span its words are cut from (the same spans a villager's orders are drawn from, roughly). */
    public record Axis(Fig fig, float lo, float hi) {
    }

    /** What the tasting spoon reads, in the order it says it. */
    public static final List<Axis> AXES = List.of(
            new Axis(Fig.HUNGER, 0f, 20f), new Axis(Fig.FAT, 0f, 1f), new Axis(Fig.FIBRE, 0f, 1f),
            new Axis(Fig.DISH_MOISTURE, 0f, 1f), new Axis(Fig.DISH_COOK, 0f, 1f), new Axis(Fig.CHARRED, 0f, 1f),
            new Axis(Fig.DISH_SALT, 0f, 1f), new Axis(Fig.DISH_SMOKE, 0f, 1f), new Axis(Fig.DISH_FRESH, 0f, 1f),
            new Axis(Fig.DISH_WARMTH, 0f, 80f),
            new Axis(Fig.SHARE_MEAT, 0f, 1f), new Axis(Fig.SHARE_SEAFOOD, 0f, 1f), new Axis(Fig.SHARE_PLANT, 0f, 1f), new Axis(Fig.SHARE_STARCH, 0f, 1f));

    /**
     * The coarse bin a village's saturation counts a dish in: what most of it is, the cuisine it leans to, and how done it is. Swapping
     * one carrot for a parsnip leaves it unchanged, so it cannot dodge saturation.
     */
    public static String bin(ModularFigures f) {
        Fig[] shares = {Fig.SHARE_MEAT, Fig.SHARE_SEAFOOD, Fig.SHARE_PLANT, Fig.SHARE_STARCH};
        Fig top = shares[0];
        for (Fig s : shares)
            if (f.get(s) > f.get(top))
                top = s;
        Fig cuisine = null;
        for (Fig c : CUISINE_FIGS)
            if (f.get(c) > 0f && (cuisine == null || f.get(c) > f.get(cuisine)))
                cuisine = c;
        String done = f.get(Fig.CHARRED) >= 0.5f ? "burnt" : f.get(Fig.DISH_COOK) >= 1f ? "done" : "raw";
        return top.name() + "|" + (cuisine == null ? "-" : cuisine.name()) + "|" + done;
    }

    /** The cuisines a dish can vote for, in the figure order. */
    private static final Material.Trait[] CUISINES = {Material.Trait.ITALIAN, Material.Trait.EAST_ASIAN, Material.Trait.MEXICAN, Material.Trait.EASTERN_EUROPEAN};
    private static final Fig[] CUISINE_FIGS = {Fig.CUISINE_ITALIAN, Fig.CUISINE_EAST_ASIAN, Fig.CUISINE_MEXICAN, Fig.CUISINE_EASTERN_EUROPEAN};

    /** Whether this stack is a dish. */
    public static boolean is(ItemStack stack) {
        ModularDesign design = ModularStacks.design(stack);
        ModularType type = design == null ? null : ModularTypes.known(design.type());
        return type != null && type.family() == ModularType.Family.DISH;
    }

    /** The stack's figures with a dish's live and composition axes filled in; any other stack's baked figures unchanged. Null if it has none. */
    public static ModularFigures read(ItemStack stack, Level level) {
        ModularFigures baked = ModularStacks.figures(stack);
        if (baked == null || !is(stack))
            return baked;
        Map<Fig, Float> out = new EnumMap<>(Fig.class);
        for (Fig f : Fig.values())
            out.put(f, baked.get(f));
        var entry = Cooking.findPerishable(level, stack);
        if (entry.isPresent()) {
            FoodState s = Cooking.current(stack, entry.get(), level, AmbientTable.at(level).value());
            out.put(Fig.DISH_COOK, s.cook());
            out.put(Fig.DISH_MOISTURE, s.moisture());
            out.put(Fig.DISH_SALT, s.salt());
            out.put(Fig.DISH_SMOKE, s.smoke());
            out.put(Fig.DISH_FRESH, 1f - s.rot());
            // What the cook has burnt stays burnt, and rot has raised the load since the bake.
            out.put(Fig.CHARRED, Math.max(baked.get(Fig.CHARRED), s.charred()));
            out.put(Fig.PATHOGEN, Math.max(baked.get(Fig.PATHOGEN), s.pathogen()));
            out.put(Fig.TOXIN, Math.max(baked.get(Fig.TOXIN), s.toxin()));
        }
        out.put(Fig.DISH_WARMTH, ItemHeat.get(stack, level).value());
        composition(ModularStacks.design(stack), out);
        return ModularFigures.of(out, baked.step());
    }

    /** Volume shares by what each food part is, and the cuisine each votes for. A pot serving counts by its ingredients' shares where it carries them. */
    private static void composition(ModularDesign design, Map<Fig, Float> out) {
        ModularType type = ModularTypes.known(design.type());
        float meat = 0, sea = 0, plant = 0, starch = 0, total = 0;
        float[] votes = new float[CUISINES.length];
        for (Part p : design.parts()) {
            if (p.role().equals(type.primary()) && !p.kind().isEmpty())
                continue; // a shaped container is no food
            Material m = MaterialTable.get().materials().get(p.material());
            if (m == null || m.food().isEmpty())
                continue;
            total++;
            var kind = m.kind().orElse(null);
            if (kind == Material.Kind.ANIMAL)
                meat++;
            else if (kind == Material.Kind.SEAFOOD)
                sea++;
            else if (kind == Material.Kind.VEGETABLE || kind == Material.Kind.FRUIT || kind == Material.Kind.FUNGUS)
                plant++;
            else if (kind == Material.Kind.GRAIN || kind == Material.Kind.DOUGH)
                starch++;
            for (int i = 0; i < CUISINES.length; i++)
                if (m.traits().contains(CUISINES[i]))
                    votes[i]++;
        }
        if (total <= 0)
            return;
        out.put(Fig.SHARE_MEAT, meat / total);
        out.put(Fig.SHARE_SEAFOOD, sea / total);
        out.put(Fig.SHARE_PLANT, plant / total);
        out.put(Fig.SHARE_STARCH, starch / total);
        for (int i = 0; i < CUISINES.length; i++)
            out.put(CUISINE_FIGS[i], votes[i] / total);
    }
}
