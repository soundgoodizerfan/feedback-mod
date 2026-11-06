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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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

    public GrappleUpgradeRecipe(CraftingBookCategory category) {
        super(category);
    }

    /** The fitting this grid makes, or null; {@code grapple[0]} receives the grapple. */
    private static Fitting read(CraftingInput input, ItemStack[] grapple) {
        grapple[0] = null;
        Item part = null;
        int count = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty())
                continue;
            if (s.is(FItems.GRAPPLE.get()) && grapple[0] == null)
                grapple[0] = s;
            else if (part == null || s.is(part)) {
                part = s.getItem();
                count++;
            } else
                return null;
        }
        if (grapple[0] == null || part == null)
            return null;
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
