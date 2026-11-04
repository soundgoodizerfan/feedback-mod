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

import io.github.soundgoodizerfan.feedback.machine.arc.ElectrodeItem;
import io.github.soundgoodizerfan.feedback.machine.arc.ElectrodeRecipe;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** The electrode recipe in the crafting tab: three bars of a material in a column, cycling through every material that can carry an arc; the output cycles with them. */
public class ElectrodeCraftingExtension implements ICraftingCategoryExtension<ElectrodeRecipe> {

    @Override
    public void setRecipe(RecipeHolder<ElectrodeRecipe> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper grid, IFocusGroup focuses) {
        List<ItemStack> bars = new ArrayList<>();
        List<ItemStack> electrodes = new ArrayList<>();
        MaterialIngredient.materialsNow().forEach((id, material) -> {
            if (material.electrodeLimit().isEmpty())
                return;
            for (String form : ElectrodeRecipe.STOCK) {
                var item = material.item(id, form);
                if (item.isPresent()) {
                    bars.add(new ItemStack(item.get()));
                    electrodes.add(ElectrodeItem.of(id));
                    return;
                }
            }
        });
        grid.createAndSetInputs(builder, List.of(bars, bars, bars), 1, 3);
        grid.createAndSetOutputs(builder, electrodes);
    }

    @Override
    public int getWidth(RecipeHolder<ElectrodeRecipe> holder) {
        return 1;
    }

    @Override
    public int getHeight(RecipeHolder<ElectrodeRecipe> holder) {
        return 3;
    }
}
