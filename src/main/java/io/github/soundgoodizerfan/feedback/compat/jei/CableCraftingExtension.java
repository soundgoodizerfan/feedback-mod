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

import io.github.soundgoodizerfan.feedback.machine.cable.CableItem;
import io.github.soundgoodizerfan.feedback.machine.cable.CableRecipe;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * A cable recipe in the crafting tab: the wire slots (bare cable; an insulating row under them coats it) cycle through every conductor, the output
 * cycles with them (same list, same order, so they stay in step), and the wire slots say what they
 * accept, not which metals happen to be loaded.
 */
public class CableCraftingExtension implements ICraftingCategoryExtension<CableRecipe> {

    @Override
    public void setRecipe(RecipeHolder<CableRecipe> holder, IRecipeLayoutBuilder builder,
                          ICraftingGridHelper grid, mezz.jei.api.recipe.IFocusGroup focuses) {
        List<ItemStack> wires = new ArrayList<>(CableRecipe.WIRE.getItems().toList());
        List<ItemStack> cables = new ArrayList<>();
        for (ItemStack wire : wires)
            Material.find(MaterialIngredient.materialsNow(), wire)
                    .ifPresent(found -> cables.add(CableItem.of(found.id(), 3)));
        List<List<ItemStack>> inputs = List.of(wires, wires, wires);
        var slots = grid.createAndSetInputs(builder, inputs, 3, 1);
        List<Component> predicate = CableRecipe.WIRE.describe();
        for (int i = 0; i < 3 && i < slots.size(); i++)
            slots.get(i).addRichTooltipCallback((view, tooltip) -> {
                predicate.forEach(tooltip::add);
                tooltip.add(Component.translatable("feedback.jei.cable.insulate"));
            });
        grid.createAndSetOutputs(builder, cables);
    }

    @Override
    public int getWidth(RecipeHolder<CableRecipe> holder) {
        return 3;
    }

    @Override
    public int getHeight(RecipeHolder<CableRecipe> holder) {
        return 1;
    }
}
