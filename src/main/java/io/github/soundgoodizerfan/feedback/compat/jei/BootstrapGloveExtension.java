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

import io.github.soundgoodizerfan.feedback.modular.BootstrapToolRecipe;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.common.Tags;

/** The bootstrap recipes in the crafting tab: only the glove (four leather in a square) is shown; the tools are vanilla's own layouts. */
public class BootstrapGloveExtension implements ICraftingCategoryExtension<BootstrapToolRecipe> {

    @Override
    public void setRecipe(RecipeHolder<BootstrapToolRecipe> holder, IRecipeLayoutBuilder builder,
                          ICraftingGridHelper grid, IFocusGroup focuses) {
        List<ItemStack> leather = List.of(Ingredient.of(Tags.Items.LEATHERS).getItems());
        ItemStack glove = BootstrapToolRecipe.glove();
        grid.createAndSetInputs(builder, List.of(leather, leather, leather, leather), 2, 2);
        grid.createAndSetOutputs(builder, List.of(glove.isEmpty() ? new ItemStack(FModular.GLOVE.get()) : glove));
    }

    @Override
    public int getWidth(RecipeHolder<BootstrapToolRecipe> holder) {
        return 2;
    }

    @Override
    public int getHeight(RecipeHolder<BootstrapToolRecipe> holder) {
        return 2;
    }
}
