// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.List;

import io.github.soundgoodizerfan.feedback.modular.item.BootstrapToolRecipe;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.Tags;

/** The bootstrap recipes in the crafting tab: only the glove (five leather, fingers over a palm) is shown; the tools are vanilla's own layouts. */
public class BootstrapGloveExtension implements ICraftingCategoryExtension<BootstrapToolRecipe> {

    @Override
    public void setRecipe(RecipeHolder<BootstrapToolRecipe> holder, IRecipeLayoutBuilder builder,
                          ICraftingGridHelper grid, IFocusGroup focuses) {
        List<ItemStack> leather = List.of(Ingredient.of(Tags.Items.LEATHERS).getItems());
        ItemStack glove = BootstrapToolRecipe.glove();
        grid.createAndSetInputs(builder, java.util.Arrays.asList(leather, null, leather, leather, leather, leather), 3, 2);
        grid.createAndSetOutputs(builder, List.of(glove.isEmpty() ? new ItemStack(FModular.GLOVE.get()) : glove));
    }

    @Override
    public int getWidth(RecipeHolder<BootstrapToolRecipe> holder) {
        return 3;
    }

    @Override
    public int getHeight(RecipeHolder<BootstrapToolRecipe> holder) {
        return 2;
    }
}
