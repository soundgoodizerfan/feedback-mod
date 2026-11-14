// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.pyro;

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
 * One crystal plate of a pyroelectric material and two wires (the electrodes), anywhere in the grid: one pyroelectric plate in
 * that material. Any plate whose {@code electrical.pyro_coupling} is above zero counts, and any wire (a form, not a metal).
 * Code, not JSON, because the result is made of whatever went in.
 */
public class PyroPlateRecipe extends CustomRecipe {

    public PyroPlateRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return read(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ResourceLocation material = read(input);
        return material == null ? ItemStack.EMPTY : PyroPlateItem.of(material);
    }

    @Nullable
    private static ResourceLocation read(CraftingInput input) {
        ResourceLocation crystal = null;
        int wires = 0;
        for (ItemStack stack : input.items()) {
            if (stack.isEmpty())
                continue;
            var found = Material.find(MaterialIngredient.materialsNow(), stack).orElse(null);
            if (found == null)
                return null;
            if (found.form().equals("wire") && found.material().kind().filter(k -> k == Material.Kind.METAL).isPresent())
                wires++;
            else if (crystal == null && found.form().equals("crystal_plate") && found.material().electrical().pyroCoupling().filter(c -> c > 0f).isPresent())
                crystal = found.id();
            else
                return null;
        }
        return crystal != null && wires == 2 ? crystal : null;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.PYRO_PLATE.get();
    }
}
