// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** One jar and one food or serving, in any arrangement, make a jar of it (food spec 8, Canning). */
public class JarFillRecipe extends CustomRecipe {

    public JarFillRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return contentSlot(input, level) >= 0;
    }

    /** The slot of the content if the grid is exactly one empty jar and one fillable thing, else -1. */
    private static int contentSlot(CraftingInput input, Level level) {
        int content = -1, jars = 0, seen = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
                continue;
            seen++;
            if (stack.is(FItems.JAR.get()))
                jars++;
            else if (Canning.fillable(level, stack) && content < 0)
                content = i;
        }
        return seen == 2 && jars == 1 ? content : -1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            // The grid's other item is the jar, so the content is the one that is not.
            if (!stack.isEmpty() && !stack.is(FItems.JAR.get()))
                return Canning.fill(stack);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.JAR_FILL.get();
    }
}
