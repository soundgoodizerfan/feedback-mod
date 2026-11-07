// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.item.GrappleUpgradeRecipe;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Each fitting's grid: a bare grapple and the fitting's part, the output cycling through the fittings. */
public class GrappleUpgradeCraftingExtension implements ICraftingCategoryExtension<GrappleUpgradeRecipe> {

    @Override
    public void setRecipe(RecipeHolder<GrappleUpgradeRecipe> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper grid, IFocusGroup focuses) {
        List<ItemStack> outputs = new ArrayList<>();
        List<List<ItemStack>> slots = new ArrayList<>(); // slot 0 the grapple, then one slot per part
        for (int i = 0; i < 5; i++)
            slots.add(new ArrayList<>());
        for (var f : GrappleUpgradeRecipe.FITTINGS) {
            slots.get(0).add(new ItemStack(FItems.GRAPPLE.get()));
            for (int i = 1; i < 5; i++)
                slots.get(i).add(i <= f.count() ? new ItemStack(f.part().get()) : ItemStack.EMPTY);
            ItemStack out = new ItemStack(FItems.GRAPPLE.get());
            out.set(FDataComponents.GRAPPLE_FITTINGS.get(), f.bit());
            outputs.add(out);
        }
        grid.createAndSetInputs(builder, slots, 3, 2);
        grid.createAndSetOutputs(builder, outputs);
    }

    @Override
    public int getWidth(RecipeHolder<GrappleUpgradeRecipe> holder) {
        return 3;
    }

    @Override
    public int getHeight(RecipeHolder<GrappleUpgradeRecipe> holder) {
        return 2;
    }
}
