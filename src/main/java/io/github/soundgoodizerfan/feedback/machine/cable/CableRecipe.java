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
package io.github.soundgoodizerfan.feedback.machine.cable;

import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialIngredient;
import io.github.soundgoodizerfan.feedback.process.MaterialProperty;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import org.jetbrains.annotations.Nullable;

/**
 * Three wires of one conductor in a row, three paper under them: three lengths of cable in that
 * conductor.
 *
 * <p>The row is {@link #WIRE}, "any wire of a metal that carries current", so no metal is named and
 * a pack's new conductor is accepted the day it has an {@code ampacity}. A recipe cannot say "the
 * output is made of whatever went in", which is why this is code and not JSON: the three wires must
 * agree, and the result carries their material.
 */
public class CableRecipe extends CustomRecipe {

    /** Any wire whose material states an ampacity at all. */
    public static final MaterialIngredient WIRE = MaterialIngredient.atLeast("wire", MaterialProperty.AMPACITY, 0f);

    public CableRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return read(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ResourceLocation material = read(input);
        return material == null ? ItemStack.EMPTY : CableItem.of(material, 3);
    }

    @Nullable
    private static ResourceLocation read(CraftingInput input) {
        if (input.width() != 3 || input.height() != 2)
            return null;
        ResourceLocation material = null;
        for (int x = 0; x < 3; x++) {
            if (!input.getItem(x, 1).is(Items.PAPER))
                return null;
            ItemStack wire = input.getItem(x, 0);
            if (!WIRE.test(wire))
                return null;
            ResourceLocation found = Material.find(MaterialIngredient.materialsNow(), wire)
                    .map(Material.Located::id).orElse(null);
            if (found == null || (material != null && !material.equals(found)))
                return null;
            material = found;
        }
        return material;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.CABLE.get();
    }
}
