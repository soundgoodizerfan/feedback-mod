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

import io.github.soundgoodizerfan.feedback.machine.tube.CarrierDyeRecipe;
import io.github.soundgoodizerfan.feedback.machine.tube.CarrierItem;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;

import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** Shows dyeing a carrier: any carrier plus a dye gives it that address. */
public final class CarrierDyeCraftingExtension implements ICraftingCategoryExtension<CarrierDyeRecipe> {

    @Override
    public void setRecipe(RecipeHolder<CarrierDyeRecipe> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper grid,
                          IFocusGroup focuses) {
        List<ItemStack> carriers = List.of(new ItemStack(FItems.CARRIER.get()), new ItemStack(FItems.MAGNETIC_CARRIER.get()));
        List<ItemStack> dyes = java.util.Arrays.stream(DyeColor.values()).map(c -> new ItemStack(DyeItem.byColor(c))).toList();
        ItemStack out = new ItemStack(FItems.CARRIER.get());
        CarrierItem.setAddress(out, java.util.Optional.of(DyeColor.RED));
        grid.createAndSetInputs(builder, List.of(carriers, dyes), 2, 1);
        grid.createAndSetOutputs(builder, List.of(out));
    }

    @Override
    public int getWidth(RecipeHolder<CarrierDyeRecipe> holder) {
        return 2;
    }

    @Override
    public int getHeight(RecipeHolder<CarrierDyeRecipe> holder) {
        return 1;
    }
}
