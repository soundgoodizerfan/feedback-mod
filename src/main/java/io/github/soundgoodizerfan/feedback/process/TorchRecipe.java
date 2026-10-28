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

import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Anything that burns, on a stick, makes four torches. "Burns" is {@link Fuel}: a material with a flame
 * temperature and a burn time, so no item is named and a pack's new fuel works the day it states them. A
 * propellant (a material with a chamber) is not a torch head, and a bucket of a melt leaves its bucket
 * behind, so gunpowder and lava stay out. Charcoal is no longer the only way in.
 */
public class TorchRecipe extends CustomRecipe {

    public TorchRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return input.width() == 1 && input.height() == 2 && input.getItem(0, 1).is(Items.STICK) && burns(input.getItem(0, 0));
    }

    private static boolean burns(ItemStack stack) {
        if (stack.isEmpty() || !stack.getCraftingRemainingItem().isEmpty())
            return false;
        return Material.find(MaterialIngredient.materialsNow(), stack)
                .filter(at -> at.material().combustion().filter(c -> c.chamber().isEmpty()).isPresent())
                .flatMap(Fuel::of).isPresent();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return new ItemStack(Items.TORCH, 4);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.TORCH.get();
    }
}
