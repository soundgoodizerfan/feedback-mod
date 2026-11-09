// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import java.util.List;
import java.util.function.Supplier;

import io.github.soundgoodizerfan.feedback.combat.tether.GrappleHook;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * A grapple and the parts of one fitting, in any arrangement -> that grapple carrying the fitting
 * ({@code grapple_fittings} bit set). A fitting is a physical part: a coil of string for line, a winch
 * for the reel, rods for a barbed head. Each fits once.
 *
 * <p>Prior art: the CurseForge "Grappling Hook Mod" (GPL; description and physics read only, no code
 * copied). Taken: upgrade-driven hook (rope length, reel speed, pulling creatures), reel while held,
 * momentum kept on release, multi-hook as a fitting (a second use throws a second line; each hook has its own tether).
 */
public class GrappleUpgradeRecipe extends CustomRecipe {

    /** A fitting: the bit it sets, its lang suffix, and the part (item, count) it takes. */
    public record Fitting(int bit, String name, Supplier<Item> part, int count) {
    }

    public static final List<Fitting> FITTINGS = List.of(
            new Fitting(GrappleHook.ROPE, "rope", () -> Items.STRING, 4),
            new Fitting(GrappleHook.WINCH, "winch", () -> FItems.GRAPPLE_WINCH.get(), 1),
            new Fitting(GrappleHook.BARB, "barb", () -> BuiltInRegistries.ITEM.get(Feedback.id("wrought_iron_rod")), 2),
            new Fitting(GrappleHook.MULTI, "multi", () -> BuiltInRegistries.ITEM.get(Feedback.id("wrought_iron_plate")), 3));

    /** Re-lining: four wires of a metal make it the grapple's line (no fitting bit). */
    public static final Fitting LINE = new Fitting(0, "line", () -> Items.AIR, 4);

    /** The material whose wire {@code stack} is, from the item's name (material id plus {@code _wire}), or null. */
    private static net.minecraft.resources.ResourceLocation wireOf(ItemStack stack) {
        net.minecraft.resources.ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key.getPath().endsWith("_wire")
                ? net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(key.getNamespace(), key.getPath().substring(0, key.getPath().length() - 5)) : null;
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
        net.minecraft.resources.ResourceLocation wire = wireOf(partStack);
        if (wire != null && count == LINE.count() && io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials().containsKey(wire)
                && io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials().get(wire).mechanical().yieldStrength().isPresent()
                && !wire.equals(grapple[0].get(FDataComponents.GRAPPLE_LINE.get())))
            return LINE;
        int have = grapple[0].getOrDefault(FDataComponents.GRAPPLE_FITTINGS.get(), 0);
        for (Fitting f : FITTINGS)
            if (f.part().get() == part && f.count() == count && (have & f.bit()) == 0)
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
                net.minecraft.resources.ResourceLocation wire = wireOf(input.getItem(i));
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
