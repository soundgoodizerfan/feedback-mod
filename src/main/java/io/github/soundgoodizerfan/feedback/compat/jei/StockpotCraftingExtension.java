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

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.machine.stockpot.StockpotItem;
import io.github.soundgoodizerfan.feedback.machine.stockpot.StockpotRecipe;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** The stockpot recipe in the crafting tab: the plate slots cycle through every conductive plate, the output cycles with them in step. */
public class StockpotCraftingExtension implements ICraftingCategoryExtension<StockpotRecipe> {

    @Override
    public void setRecipe(RecipeHolder<StockpotRecipe> holder, IRecipeLayoutBuilder builder,
                          ICraftingGridHelper grid, mezz.jei.api.recipe.IFocusGroup focuses) {
        List<ItemStack> plates = new ArrayList<>();
        List<ItemStack> pots = new ArrayList<>();
        MaterialIngredient.materialsNow().forEach((id, material) -> {
            var plate = material.items(id).get("plate");
            if (plate != null && material.thermal().conductivity().isPresent()) {
                plates.add(new ItemStack(plate));
                pots.add(StockpotItem.of(id));
            }
        });
        grid.createAndSetInputs(builder, List.of(plates, List.of(), plates, plates, plates, plates), 3, 2);
        grid.createAndSetOutputs(builder, pots);
    }

    @Override
    public int getWidth(RecipeHolder<StockpotRecipe> holder) {
        return 3;
    }

    @Override
    public int getHeight(RecipeHolder<StockpotRecipe> holder) {
        return 2;
    }
}
