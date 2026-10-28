/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
 * Two or more of one food in the grid make that many of it again, every figure taken from the freshest of them
 * (the lowest spoilage), so the result stacks as one. Vanilla refuses to stack foods whose states differ by a
 * few minutes of rot. Only plain foods ({@link Cooking#find}); a dish is not merged.
 */
public class FoodMergeRecipe extends CustomRecipe {

    public FoodMergeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int seen = 0;
        ItemStack first = ItemStack.EMPTY;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
                continue;
            if (first.isEmpty()) {
                first = stack;
                if (Cooking.find(level, stack).isEmpty())
                    return false;
            } else if (stack.getItem() != first.getItem())
                return false;
            seen++;
        }
        return seen >= 2;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null)
            return ItemStack.EMPTY;
        // shortcut: CraftingInput carries no level or player, so the overworld clock and ambient stand in; fix if a dimension's ambient ever matters here.
        Level level = server.overworld();
        float env = AmbientTable.at(level).value();
        ItemStack out = ItemStack.EMPTY;
        FoodState low = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
                continue;
            ItemStack copy = stack.copyWithCount(1);
            Cooking.settle(copy, level, env);
            FoodState state = copy.get(FDataComponents.FOOD_STATE.get());
            if (state == null)
                return ItemStack.EMPTY;
            low = low == null ? state : FoodEvents.lowest(low, state);
            out = out.isEmpty() ? copy : out.copyWithCount(out.getCount() + 1);
        }
        var entry = Cooking.find(level, out);
        if (entry.isEmpty() || low == null)
            return ItemStack.EMPTY;
        out.set(FDataComponents.FOOD_STATE.get(), low);
        Cooking.apply(out, entry.get(), low);
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.FOOD_MERGE.get();
    }
}
