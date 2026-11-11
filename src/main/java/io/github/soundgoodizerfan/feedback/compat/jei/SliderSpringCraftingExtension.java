// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.bearing.SliderSpringRecipe;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;

/** A slider and four wires of one metal; the grid cycles through a soft, a stock and a stiff metal's wire, each with its spring on the output. */
public class SliderSpringCraftingExtension implements ICraftingCategoryExtension<SliderSpringRecipe> {

    private static final String[] METALS = {"lead", "iron", "steel"};

    @Override
    public void setRecipe(RecipeHolder<SliderSpringRecipe> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper grid, IFocusGroup focuses) {
        List<ItemStack> outputs = new ArrayList<>();
        List<List<ItemStack>> slots = new ArrayList<>();
        for (int i = 0; i < 5; i++)
            slots.add(new ArrayList<>());
        for (String metal : METALS) {
            Item wire = BuiltInRegistries.ITEM.get(Feedback.id(metal + "_wire"));
            if (wire == Items.AIR)
                continue;
            slots.get(0).add(new ItemStack(FItems.SLIDER_JOINT.get()));
            for (int i = 1; i < 5; i++)
                slots.get(i).add(new ItemStack(wire));
            ItemStack out = new ItemStack(FItems.SLIDER_JOINT.get());
            out.set(FDataComponents.SLIDER_SPRING.get(), Feedback.id(metal));
            outputs.add(out);
        }
        grid.createAndSetInputs(builder, slots, 3, 2);
        grid.createAndSetOutputs(builder, outputs);
    }

    @Override
    public int getWidth(RecipeHolder<SliderSpringRecipe> holder) {
        return 3;
    }

    @Override
    public int getHeight(RecipeHolder<SliderSpringRecipe> holder) {
        return 2;
    }
}
