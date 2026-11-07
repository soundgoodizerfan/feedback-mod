// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.arc;

import java.util.Set;

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
 * Three bars of one electrode-capable material in a column: one electrode of that material. Which materials qualify is the material's
 * ({@link Material#electrodeLimit}: graphite sublimates, a conducting metal boils), so no material is named and a pack's new conductor
 * qualifies the day it states a boiling point. A recipe cannot say "the output is made of whatever went in", which is why this is code.
 */
public class ElectrodeRecipe extends CustomRecipe {

    /** The forms of stock a rod can be cut from. */
    public static final Set<String> STOCK = Set.of("rod", "ingot", "lump");

    public ElectrodeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return read(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ResourceLocation material = read(input);
        return material == null ? ItemStack.EMPTY : ElectrodeItem.of(material);
    }

    @Nullable
    private static ResourceLocation read(CraftingInput input) {
        if (input.width() != 1 || input.height() != 3)
            return null;
        ResourceLocation material = null;
        for (int y = 0; y < 3; y++) {
            var found = Material.find(MaterialIngredient.materialsNow(), input.getItem(0, y));
            if (found.isEmpty() || !STOCK.contains(found.get().form()) || found.get().material().electrodeLimit().isEmpty())
                return null;
            if (material != null && !material.equals(found.get().id()))
                return null;
            material = found.get().id();
        }
        return material;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.ARC_ELECTRODE.get();
    }
}
