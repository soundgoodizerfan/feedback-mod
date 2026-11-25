// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * A mortar and one rod that stores heat, ground. A charged rod (still over the heat that makes it hot, {@link RodCharge#charged}) gives
 * powder that burns: the heat it stored is the fuel. A spent rod ({@link RodCharge#spent}: the spent item, or a rod that has gone cold)
 * gives dust with nothing left to burn. Told apart by the rod's heat and never by what it is called, so letting a rod go cold first is the
 * cost of the safe grind. The mortar is kept.
 */
public class RodGrindRecipe extends CustomRecipe {
    private final boolean charged;

    public RodGrindRecipe(CraftingBookCategory category, boolean charged) {
        super(category);
        this.charged = charged;
    }

    /** The one rod in the grid, with the mortar beside it and nothing else, or empty. */
    private ItemStack rod(CraftingInput input) {
        ItemStack rod = ItemStack.EMPTY;
        boolean mortar = false;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
                continue;
            if (stack.is(FItems.MORTAR.get()) && !mortar)
                mortar = true;
            else if ((stack.is(FTags.CHARGED_RODS) || stack.is(FTags.SPENT_RODS)) && rod.isEmpty())
                rod = stack;
            else
                return ItemStack.EMPTY;
        }
        return mortar ? rod : ItemStack.EMPTY;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        ItemStack rod = rod(input);
        if (rod.isEmpty())
            return false;
        return charged ? RodCharge.charged(rod, level) : RodCharge.spent(rod, level);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return charged ? new ItemStack(Items.BLAZE_POWDER, 2) : new ItemStack(FItems.INERT_BLAZE_DUST.get(), 2);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> left = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++)
            if (input.getItem(i).is(FItems.MORTAR.get()))
                left.set(i, input.getItem(i).copyWithCount(1));
        return left;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return charged ? FRecipes.ROD_GRIND_CHARGED.get() : FRecipes.ROD_GRIND_SPENT.get();
    }
}
