// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.machine.skillet.SkilletItem;
import io.github.soundgoodizerfan.feedback.machine.skillet.SkilletRecipe;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

/** The skillet recipe in the crafting tab: the plate slots cycle through every conductive plate, the output cycles with them in step. */
public class SkilletCraftingExtension implements ICraftingCategoryExtension<SkilletRecipe> {

    @Override
    public void setRecipe(RecipeHolder<SkilletRecipe> holder, IRecipeLayoutBuilder builder,
                          ICraftingGridHelper grid, mezz.jei.api.recipe.IFocusGroup focuses) {
        List<ItemStack> plates = new ArrayList<>();
        List<ItemStack> pans = new ArrayList<>();
        MaterialIngredient.materialsNow().forEach((id, material) -> {
            var plate = material.items(id).get("plate");
            if (plate != null && material.thermal().conductivity().isPresent()) {
                plates.add(new ItemStack(plate));
                pans.add(SkilletItem.of(id));
            }
        });
        List<ItemStack> stick = List.of(new ItemStack(Items.STICK));
        grid.createAndSetInputs(builder, List.of(plates, plates, plates, List.of(), stick, List.of()), 3, 2);
        grid.createAndSetOutputs(builder, pans);
    }

    @Override
    public int getWidth(RecipeHolder<SkilletRecipe> holder) {
        return 3;
    }

    @Override
    public int getHeight(RecipeHolder<SkilletRecipe> holder) {
        return 2;
    }
}
