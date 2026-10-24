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
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Cut;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import java.util.Map;

/**
 * A crystal plate that rings (some {@code piezo_coupling}) and a metal plate for backing, in any
 * arrangement, make a mass probe carrying the plate's cut (mechanics 6.17). Nothing is named: the
 * plate is a {@code crystal_plate} form and the backing a {@code plate} form of some material, and
 * only metals have a plate form, which is also why only metal conducts.
 */
public class MassProbeRecipe extends CustomRecipe {

    public MassProbeRecipe(CraftingBookCategory category) {
        super(category);
    }

    /** The crystal plate in the grid, or null if the grid is not exactly one ringing plate and one backing. */
    private static ItemStack read(CraftingInput input, Map<ResourceLocation, Material> materials) {
        ItemStack crystal = null;
        int backing = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
                continue;
            Material.Located found = Material.find(materials, stack.getItem()).orElse(null);
            if (found == null)
                return null;
            if (found.form().equals("crystal_plate")) {
                if (crystal != null || found.material().electrical().piezoCoupling().orElse(0f) < 0.5f)
                    return null;
                crystal = stack;
            } else if (found.form().equals("plate"))
                backing++;
            else
                return null;
        }
        return backing == 1 ? crystal : null;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return read(input, ClientMaterials.on(level)) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack crystal = read(input, MaterialTable.get().materials());
        if (crystal == null)
            return ItemStack.EMPTY;
        ItemStack out = new ItemStack(FItems.MASS_PROBE.get());
        out.set(FDataComponents.CUT.get(), Cut.of(crystal));
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.MASS_PROBE.get();
    }
}
