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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.registry.FRecipes;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * A magazine and loose rounds make that magazine, fuller. Hand loading, the grid's way: one
 * magazine plus one to ten cartridges, all one bore, the magazine left with room for them.
 */
public class MagazineLoadRecipe extends CustomRecipe {

    public MagazineLoadRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return result(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        Magazine m = result(input);
        if (m == null)
            return ItemStack.EMPTY;
        ItemStack out = magazineIn(input).copyWithCount(1);
        out.set(FWeapons.MAGAZINE.get(), m);
        return out;
    }

    /** The one magazine stack in the grid; {@link #result} has already checked there is exactly one. */
    private static ItemStack magazineIn(CraftingInput input) {
        for (int i = 0; i < input.size(); i++)
            if (input.getItem(i).is(FWeapons.MAGAZINE_ITEM.get()))
                return input.getItem(i);
        return ItemStack.EMPTY;
    }

    /** The magazine this grid makes, or null if it is not one magazine plus cartridges that fit. */
    @Nullable
    private static Magazine result(CraftingInput input) {
        Magazine magazine = null;
        int capacity = 0;
        java.util.List<Cartridge> rounds = new java.util.ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty())
                continue;
            if (s.is(FWeapons.MAGAZINE_ITEM.get())) {
                if (magazine != null)
                    return null;
                magazine = MagazineItem.rounds(s);
                capacity = GunAmmo.size(s).capacity();
            } else if (s.is(FWeapons.CARTRIDGE_ITEM.get()) && s.has(FWeapons.CARTRIDGE.get())) {
                rounds.add(s.get(FWeapons.CARTRIDGE.get()));
            } else
                return null;
        }
        if (magazine == null || rounds.isEmpty())
            return null;
        for (Cartridge r : rounds) {
            if (!magazine.accepts(r, capacity))
                return null;
            magazine = magazine.push(r);
        }
        return magazine;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.MAGAZINE_LOAD.get();
    }
}
