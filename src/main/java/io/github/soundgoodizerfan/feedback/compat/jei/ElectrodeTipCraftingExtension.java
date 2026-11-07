// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jei;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.machine.arc.ElectrodeItem;
import io.github.soundgoodizerfan.feedback.machine.arc.ElectrodeRecipe;
import io.github.soundgoodizerfan.feedback.machine.arc.ElectrodeTipRecipe;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

/** A tip bar over an electrode: every (rod, tip) pair where the tip can take more heat than the rod, the output cycling with them. */
public class ElectrodeTipCraftingExtension implements ICraftingCategoryExtension<ElectrodeTipRecipe> {

    @Override
    public void setRecipe(RecipeHolder<ElectrodeTipRecipe> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper grid, IFocusGroup focuses) {
        List<ItemStack> bars = new ArrayList<>(), rods = new ArrayList<>(), outputs = new ArrayList<>();
        var materials = MaterialIngredient.materialsNow();
        for (var rod : materials.entrySet()) {
            var rodLimit = rod.getValue().electrodeLimit();
            if (rodLimit.isEmpty())
                continue;
            for (var tip : materials.entrySet()) {
                var tipLimit = tip.getValue().electrodeLimit();
                if (tipLimit.isEmpty() || tipLimit.get().value() <= rodLimit.get().value())
                    continue;
                for (String form : ElectrodeRecipe.STOCK) {
                    var item = tip.getValue().item(tip.getKey(), form);
                    if (item.isPresent()) {
                        bars.add(new ItemStack(item.get()));
                        rods.add(ElectrodeItem.of(rod.getKey()));
                        outputs.add(ElectrodeItem.tipped(rod.getKey(), tip.getKey()));
                        break;
                    }
                }
            }
        }
        grid.createAndSetInputs(builder, List.of(bars, rods), 1, 2);
        grid.createAndSetOutputs(builder, outputs);
    }

    @Override
    public int getWidth(RecipeHolder<ElectrodeTipRecipe> holder) {
        return 1;
    }

    @Override
    public int getHeight(RecipeHolder<ElectrodeTipRecipe> holder) {
        return 2;
    }
}
