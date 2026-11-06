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
package io.github.soundgoodizerfan.feedback.machine.tube;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import io.github.soundgoodizerfan.feedback.registry.FRecipes;

/** One carrier (any kind, loaded or not) and one dye: the carrier keeps its contents and takes the dye as its address. */
public class CarrierDyeRecipe extends CustomRecipe {

    public CarrierDyeRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int carriers = 0, dyes = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty())
                continue;
            if (s.getItem() instanceof CarrierItem && ++carriers > 1)
                return false;
            else if (s.getItem() instanceof DyeItem && ++dyes > 1)
                return false;
            else if (!(s.getItem() instanceof CarrierItem) && !(s.getItem() instanceof DyeItem))
                return false;
        }
        return carriers == 1 && dyes == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack out = ItemStack.EMPTY;
        DyeColor color = DyeColor.WHITE;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.getItem() instanceof CarrierItem)
                out = s.copyWithCount(1);
            else if (s.getItem() instanceof DyeItem d)
                color = d.getDyeColor();
        }
        CarrierItem.setAddress(out, java.util.Optional.of(color));
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.CARRIER_DYE.get();
    }
}
