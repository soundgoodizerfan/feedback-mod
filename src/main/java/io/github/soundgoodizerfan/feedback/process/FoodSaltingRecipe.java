// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * One food and one salt, in any arrangement, make that food salted (food spec 8): its salt goes to 1 and the
 * salt draws water out of it. A food is whatever {@link Cooking#find} indexes and a salt is any material with
 * the {@code saline} trait, so no item is named.
 */
public class FoodSaltingRecipe extends CustomRecipe {

    public FoodSaltingRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return foodSlot(input, level) >= 0;
    }

    /** The slot of the food if the grid is exactly one food and one salt, else -1. */
    private static int foodSlot(CraftingInput input, Level level) {
        int food = -1, salt = 0, seen = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
                continue;
            seen++;
            if (Material.find(ClientMaterials.on(level), stack).filter(l -> l.material().traits().contains(Material.Trait.SALINE)).isPresent())
                salt++;
            else if (Cooking.find(level, stack).isPresent() && food < 0)
                food = i;
        }
        return seen == 2 && salt == 1 ? food : -1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        // No level reaches assemble, so the server's overworld settles the spoilage first: salting must not shield the rot already earned.
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return ItemStack.EMPTY;
        // shortcut: CraftingInput carries no level or player, so the overworld clock and ambient stand in; fix if a dimension's ambient ever matters here.
        Level level = server.overworld();
        int slot = foodSlot(input, level);
        if (slot < 0)
            return ItemStack.EMPTY;
        ItemStack out = input.getItem(slot).copyWithCount(1);
        Cooking.find(level, out).ifPresent(entry -> {
            FoodState salted = Cooking.salted(Cooking.current(out, entry, level, AmbientTable.at(level).value()));
            out.set(FDataComponents.FOOD_STATE.get(), salted);
            Cooking.apply(out, entry, salted);
        });
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.FOOD_SALTING.get();
    }
}
