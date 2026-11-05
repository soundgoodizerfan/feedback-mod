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
package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.ender.EnderPairRecipe;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.api.recipe.IFocusGroup;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Shows the bonded-pair recipe: a pearl and two metal plates give two ports of one bond. */
public final class EnderPairCraftingExtension implements ICraftingCategoryExtension<EnderPairRecipe> {

    @Override
    public void setRecipe(RecipeHolder<EnderPairRecipe> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper grid,
                          IFocusGroup focuses) {
        ItemStack plate = new ItemStack(BuiltInRegistries.ITEM.get(Feedback.id("steel_plate")));
        grid.createAndSetInputs(builder, List.of(List.of(new ItemStack(Items.ENDER_PEARL)), List.of(plate), List.of(plate)), 3, 1);
        grid.createAndSetOutputs(builder, List.of(new ItemStack(FItems.ENDER_PORT.get(), 2)));
    }

    @Override
    public int getWidth(RecipeHolder<EnderPairRecipe> holder) {
        return 3;
    }

    @Override
    public int getHeight(RecipeHolder<EnderPairRecipe> holder) {
        return 1;
    }
}
