// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.stockpot;

import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

/**
 * Five plates of one material in a U (three along the bottom, one up each side): a stockpot of that material. Any plate counts
 * provided its material conducts heat, so no material is named and a pack's new metal is accepted the day it has a plate.
 */
public class StockpotRecipe extends CustomRecipe {

    public StockpotRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return read(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ResourceLocation material = read(input);
        return material == null ? ItemStack.EMPTY : StockpotItem.of(material);
    }

    @Nullable
    private static ResourceLocation read(CraftingInput input) {
        if (input.width() != 3 || input.height() != 2)
            return null;
        if (!input.getItem(1, 0).isEmpty())
            return null;
        ResourceLocation material = null;
        for (int i = 0; i < 6; i++) {
            if (i == 1)
                continue;
            var found = Material.find(MaterialIngredient.materialsNow(), input.getItem(i % 3, i / 3)).orElse(null);
            if (found == null || !found.form().equals("plate") || found.material().thermal().conductivity().isEmpty())
                return null;
            if (material != null && !material.equals(found.id()))
                return null;
            material = found.id();
        }
        return material;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.STOCKPOT.get();
    }
}
