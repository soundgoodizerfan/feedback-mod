// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.item.HotToolRecipe;
import io.github.soundgoodizerfan.feedback.registry.FModular;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** A hot-hammered tool: the hammer and the pieces, with a tooltip saying the pieces must be at working temperature. */
public class HotToolCraftingExtension implements ICraftingCategoryExtension<HotToolRecipe> {

    @Override
    public void setRecipe(RecipeHolder<HotToolRecipe> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper grid, IFocusGroup focuses) {
        HotToolRecipe recipe = holder.value();
        List<List<ItemStack>> inputs = new ArrayList<>();
        inputs.add(List.of(new ItemStack(FModular.HAFTED_TOOL.get())));
        ItemStack piece = new ItemStack(BuiltInRegistries.ITEM.get(recipe.piece()));
        for (int i = 0; i < recipe.count(); i++)
            inputs.add(List.of(piece));
        var slots = grid.createAndSetInputs(builder, inputs, 3, 2);
        for (int i = 1; i < slots.size(); i++)
            slots.get(i).addRichTooltipCallback((view, tooltip) -> tooltip.add(Component.translatable("feedback.jei.hot_tool.hot")));
        grid.createAndSetOutputs(builder, List.of(recipe.result()));
    }

    @Override
    public int getWidth(RecipeHolder<HotToolRecipe> holder) {
        return 3;
    }

    @Override
    public int getHeight(RecipeHolder<HotToolRecipe> holder) {
        return 2;
    }
}
