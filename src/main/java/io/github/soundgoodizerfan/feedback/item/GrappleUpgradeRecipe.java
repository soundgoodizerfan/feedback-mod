// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import io.github.soundgoodizerfan.feedback.combat.tether.GrappleHook;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;
import io.github.soundgoodizerfan.feedback.process.MaterialProperty;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * A grapple and the parts of one fitting, in any arrangement -> that grapple carrying the fitting
 * ({@code grapple_fittings} bit set). A fitting is a physical part, told by form and strength: a string of enough
 * yield strength for line, a winch for the reel, rods and plates of wrought iron's strength for a barbed or multi head. Each fits once.
 *
 * <p>Prior art: the CurseForge "Grappling Hook Mod" (GPL; description and physics read only, no code
 * copied). Taken: upgrade-driven hook (rope length, reel speed, pulling creatures), reel while held,
 * momentum kept on release, multi-hook as a fitting (a second use throws a second line; each hook has its own tether).
 */
public class GrappleUpgradeRecipe extends CustomRecipe {

    /** Yield strength a fitting's string must have (wool's) and its rods and plates (wrought iron's). Placeholders. */
    private static final float ROPE_MIN_YIELD = 3f;
    private static final float HARD_MIN_YIELD = 8f;

    /** A fitting: the bit it sets, its lang suffix, what part it takes (a test and the stacks a recipe browser shows), and how many. */
    public record Fitting(int bit, String name, Predicate<ItemStack> part, Supplier<List<ItemStack>> shown, int count) {
        static Fitting of(int bit, String name, MaterialIngredient need, int count) {
            return new Fitting(bit, name, need::test, () -> need.getItems().toList(), count);
        }
    }

    public static final List<Fitting> FITTINGS = List.of(
            Fitting.of(GrappleHook.ROPE, "rope", MaterialIngredient.atLeast("string", MaterialProperty.YIELD_STRENGTH, ROPE_MIN_YIELD), 4),
            new Fitting(GrappleHook.WINCH, "winch", s -> s.is(FItems.GRAPPLE_WINCH.get()), () -> List.of(new ItemStack(FItems.GRAPPLE_WINCH.get())), 1),
            Fitting.of(GrappleHook.BARB, "barb", MaterialIngredient.atLeast("rod", MaterialProperty.YIELD_STRENGTH, HARD_MIN_YIELD), 2),
            Fitting.of(GrappleHook.MULTI, "multi", MaterialIngredient.atLeast("plate", MaterialProperty.YIELD_STRENGTH, HARD_MIN_YIELD), 3));

    /** Re-lining: four wires of a metal make it the grapple's line (no fitting bit). */
    public static final Fitting LINE = new Fitting(0, "line", s -> false, List::of, 4);

    /** The material whose wire {@code stack} is (any material with a yield strength), or null. */
    private static ResourceLocation wireOf(ItemStack stack) {
        return MaterialIngredient.locate(stack.getItem())
                .filter(l -> l.form().equals("wire") && l.material().mechanical().yieldStrength().isPresent())
                .map(Material.Located::id).orElse(null);
    }

    public GrappleUpgradeRecipe(CraftingBookCategory category) {
        super(category);
    }

    /** The fitting this grid makes, or null; {@code grapple[0]} receives the grapple. */
    private static Fitting read(CraftingInput input, ItemStack[] grapple) {
        grapple[0] = null;
        Item part = null;
        ItemStack partStack = ItemStack.EMPTY;
        int count = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty())
                continue;
            if (s.is(FItems.GRAPPLE.get()) && grapple[0] == null)
                grapple[0] = s;
            else if (part == null || s.is(part)) {
                part = s.getItem();
                partStack = s;
                count++;
            } else
                return null;
        }
        if (grapple[0] == null || part == null)
            return null;
        ResourceLocation wire = wireOf(partStack);
        if (wire != null && count == LINE.count() && !wire.equals(grapple[0].get(FDataComponents.GRAPPLE_LINE.get())))
            return LINE;
        int have = grapple[0].getOrDefault(FDataComponents.GRAPPLE_FITTINGS.get(), 0);
        for (Fitting f : FITTINGS)
            if (f.part().test(partStack) && f.count() == count && (have & f.bit()) == 0)
                return f;
        return null;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return read(input, new ItemStack[1]) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack[] grapple = new ItemStack[1];
        Fitting f = read(input, grapple);
        if (f == null)
            return ItemStack.EMPTY;
        ItemStack out = grapple[0].copyWithCount(1);
        if (f == LINE) {
            for (int i = 0; i < input.size(); i++) {
                ResourceLocation wire = wireOf(input.getItem(i));
                if (wire != null && !input.getItem(i).is(FItems.GRAPPLE.get())) {
                    out.set(FDataComponents.GRAPPLE_LINE.get(), wire);
                    break;
                }
            }
            return out;
        }
        out.set(FDataComponents.GRAPPLE_FITTINGS.get(), out.getOrDefault(FDataComponents.GRAPPLE_FITTINGS.get(), 0) | f.bit());
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 5;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.GRAPPLE_UPGRADE.get();
    }
}
