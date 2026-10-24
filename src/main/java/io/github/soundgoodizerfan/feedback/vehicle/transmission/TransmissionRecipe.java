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
package io.github.soundgoodizerfan.feedback.vehicle.transmission;

import io.github.soundgoodizerfan.feedback.registry.FRecipes;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/** A Transmission Blueprint and its material's ingots make that Transmission; the Blueprint is kept (the engine recipe's shape). */
public class TransmissionRecipe extends CustomRecipe {

    public TransmissionRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        TransmissionDesign design = blueprint(input);
        if (design == null)
            return false;
        var material = io.github.soundgoodizerfan.feedback.process.ClientMaterials.on(level).get(design.material());
        Item ingot = material == null ? null : material.item(design.material(), "ingot").orElse(null);
        if (ingot == null)
            return false;
        int ingots = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack s = input.getItem(i);
            if (s.isEmpty() || s.is(FVehicles.TRANSMISSION_BLUEPRINT.get()))
                continue;
            if (!s.is(ingot))
                return false;
            ingots++;
        }
        return ingots == design.ingots();
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        TransmissionDesign design = blueprint(input);
        if (design == null)
            return ItemStack.EMPTY;
        ItemStack out = new ItemStack(FVehicles.TRANSMISSION_ITEM.get());
        out.set(FVehicles.TRANSMISSION_DESIGN.get(), design);
        return out;
    }

    @org.jetbrains.annotations.Nullable
    private static TransmissionDesign blueprint(CraftingInput input) {
        TransmissionDesign design = null;
        for (int i = 0; i < input.size(); i++) {
            TransmissionDesign d = input.getItem(i).get(FVehicles.TRANSMISSION_DESIGN.get());
            if (d != null && input.getItem(i).is(FVehicles.TRANSMISSION_BLUEPRINT.get())) {
                if (design != null)
                    return null;
                design = d;
            }
        }
        return design;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> out = NonNullList.withSize(input.size(), ItemStack.EMPTY);
        for (int i = 0; i < input.size(); i++)
            if (input.getItem(i).is(FVehicles.TRANSMISSION_BLUEPRINT.get()))
                out.set(i, input.getItem(i).copyWithCount(1));
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.TRANSMISSION.get();
    }
}
