// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.levitation;

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
 * Two plates of one diamagnetic material side by side: one levitation plate in that material. No material is named: any
 * plate whose {@code separation.diamagnetism} is above zero counts, so a pack's new diamagnet is accepted the day it has
 * one. Code, not JSON, because the result is made of whatever went in.
 */
public class LevitationPlateRecipe extends CustomRecipe {

    public LevitationPlateRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return read(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ResourceLocation material = read(input);
        return material == null ? ItemStack.EMPTY : LevitationPlateItem.of(material);
    }

    @Nullable
    private static ResourceLocation read(CraftingInput input) {
        if (input.width() != 2 || input.height() != 1)
            return null;
        ResourceLocation material = null;
        for (int x = 0; x < 2; x++) {
            var found = Material.find(MaterialIngredient.materialsNow(), input.getItem(x, 0)).orElse(null);
            if (found == null || !found.form().equals("plate") || found.material().separation().diamagnetism() <= 0f)
                return null;
            if (material != null && !material.equals(found.id()))
                return null;
            material = found.id();
        }
        return material;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.LEVITATION_PLATE.get();
    }
}
