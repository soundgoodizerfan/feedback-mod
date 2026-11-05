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
package io.github.soundgoodizerfan.feedback.machine.breaker;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.cable.CableGauge;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * The breaker's recipe with cable where the wire goes: ceramic, a lever and a wrought iron plate round two
 * cables of one gauge, and the breaker takes that gauge (its contacts are as thick as the cable it
 * is spliced into). The breaker from copper wire stays a data recipe and is a standard one.
 */
public class BreakerRecipe extends CustomRecipe {

    public BreakerRecipe(CraftingBookCategory category) {
        super(category);
    }

    /** The two cables' gauge, or null if the grid is not the breaker. */
    private static CableGauge read(CraftingInput input) {
        if (input.width() != 3 || input.height() != 3)
            return null;
        for (int x = 0; x < 3; x++) {
            if (!input.getItem(x, 2).is(FItems.CERAMIC.get()))
                return null;
        }
        if (!input.getItem(0, 0).is(FItems.CERAMIC.get()) || !input.getItem(2, 0).is(FItems.CERAMIC.get())
                || !input.getItem(1, 0).is(Items.LEVER))
            return null;
        if (!input.getItem(1, 1).is(BuiltInRegistries.ITEM.get(Feedback.id("wrought_iron_plate"))))
            return null;
        ItemStack a = input.getItem(0, 1), b = input.getItem(2, 1);
        if (!a.is(FItems.CABLE.get()) || !b.is(FItems.CABLE.get()))
            return null;
        CableGauge ga = a.getOrDefault(FDataComponents.CABLE_GAUGE.get(), CableGauge.STANDARD);
        return ga == b.getOrDefault(FDataComponents.CABLE_GAUGE.get(), CableGauge.STANDARD) ? ga : null;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return read(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        CableGauge gauge = read(input);
        if (gauge == null)
            return ItemStack.EMPTY;
        ItemStack out = new ItemStack(FItems.BREAKER.get());
        if (gauge != CableGauge.STANDARD)
            out.set(FDataComponents.CABLE_GAUGE.get(), gauge);
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.BREAKER.get();
    }
}
