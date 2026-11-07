// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.xp;

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
 * Three items of one material in a row: three lengths of XP wire in that material. Any form counts (a sheet, a
 * gem, a vein) provided the material conducts XP ({@code xp_store.conductance} above zero), so no material is named
 * and a pack's new conductor is accepted the day it has one. Code, not JSON, because the result is made of whatever
 * went in.
 */
public class XpWireRecipe extends CustomRecipe {

    public XpWireRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return read(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ResourceLocation material = read(input);
        if (material == null)
            return ItemStack.EMPTY;
        ItemStack out = XpWireItem.of(material);
        out.setCount(3);
        return out;
    }

    @Nullable
    private static ResourceLocation read(CraftingInput input) {
        if (input.width() != 3 || input.height() != 1)
            return null;
        ResourceLocation material = null;
        for (int x = 0; x < 3; x++) {
            ItemStack stack = input.getItem(x, 0);
            var found = Material.find(MaterialIngredient.materialsNow(), stack).orElse(null);
            if (found == null || found.material().xpStore().filter(c -> c.conductance() > 0f).isEmpty())
                return null;
            if (material != null && !material.equals(found.id()))
                return null;
            material = found.id();
        }
        return material;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.XP_WIRE.get();
    }
}
