// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.treatise.BootstrapSpecimenRecipe;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

/** A specimen recipe in the crafting tab: its grid as laid out, and the baked specimen it hands out. */
public class BootstrapSpecimenExtension implements ICraftingCategoryExtension<BootstrapSpecimenRecipe> {

    @Override
    public void setRecipe(RecipeHolder<BootstrapSpecimenRecipe> holder, IRecipeLayoutBuilder builder,
                          ICraftingGridHelper grid, IFocusGroup focuses) {
        BootstrapSpecimenRecipe recipe = holder.value();
        List<List<ItemStack>> inputs = new ArrayList<>();
        for (Ingredient i : recipe.getIngredients())
            inputs.add(i.isEmpty() ? null : List.of(i.getItems()));
        grid.createAndSetInputs(builder, inputs, getWidth(holder), getHeight(holder));
        grid.createAndSetOutputs(builder, List.of(recipe.specimen()));
    }

    @Override
    public int getWidth(RecipeHolder<BootstrapSpecimenRecipe> holder) {
        return holder.value().getWidth();
    }

    @Override
    public int getHeight(RecipeHolder<BootstrapSpecimenRecipe> holder) {
        return holder.value().getHeight();
    }
}
