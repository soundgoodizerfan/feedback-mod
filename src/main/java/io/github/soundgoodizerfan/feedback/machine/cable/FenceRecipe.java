// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.cable;

import java.util.Optional;

import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * A cable on two rods, in a column: two fence posts carrying that cable's conductor. The rods
 * are the post (a bar of metal genuinely belongs in it) and may be any material's; the wire in
 * the post is whatever the cable is made of, at its gauge, so it is code and not JSON, like {@link CableRecipe}.
 */
public class FenceRecipe extends CustomRecipe {

    private static final MaterialIngredient ROD = new MaterialIngredient(Optional.of("rod"), Optional.empty(),
            Optional.empty(), Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);

    public FenceRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static MadeOf read(CraftingInput input) {
        if (input.width() != 1 || input.height() != 3)
            return null;
        ItemStack cable = input.getItem(0, 0);
        if (!cable.is(FItems.CABLE.get()) || !ROD.test(input.getItem(0, 1)) || !ROD.test(input.getItem(0, 2)))
            return null;
        return cable.get(FDataComponents.MADE_OF.get());
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return read(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        MadeOf made = read(input);
        if (made == null)
            return ItemStack.EMPTY;
        ItemStack out = new ItemStack(FItems.FENCE.get(), 2);
        out.set(FDataComponents.MADE_OF.get(), made);
        CableGauge gauge = input.getItem(0, 0).get(FDataComponents.CABLE_GAUGE.get());
        if (gauge != null)
            out.set(FDataComponents.CABLE_GAUGE.get(), gauge);
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 1 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.FENCE.get();
    }
}
