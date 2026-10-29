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

import io.github.soundgoodizerfan.feedback.process.Circuit;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.registry.FRecipes;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * An unfitted instrument of the {@code circuit_fittable} tag and one live circuit, in any
 * arrangement, make that instrument carrying the circuit. The circuit rides on the instrument as
 * its own {@code circuit} component and {@code Instruments.fitted} narrows the step by its noise,
 * so a quiet circuit helps most, a noisy one little and a dead one is refused. Nothing gates:
 * the bare instrument still reads, just coarser.
 */
public class InstrumentFitRecipe extends CustomRecipe {

    public InstrumentFitRecipe(CraftingBookCategory category) {
        super(category);
    }

    /** The instrument in the grid, or null unless the grid is exactly one unfitted instrument and one live circuit. */
    private static ItemStack read(CraftingInput input, ItemStack[] circuit) {
        ItemStack instrument = null;
        circuit[0] = null;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty())
                continue;
            if (stack.is(FItems.CIRCUIT.get()) && circuit[0] == null && Circuit.of(stack).live())
                circuit[0] = stack;
            else if (stack.is(FTags.CIRCUIT_FITTABLE) && instrument == null && !Circuit.of(stack).live())
                instrument = stack;
            else
                return null;
        }
        return circuit[0] == null ? null : instrument;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return read(input, new ItemStack[1]) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack[] circuit = new ItemStack[1];
        ItemStack instrument = read(input, circuit);
        if (instrument == null)
            return ItemStack.EMPTY;
        ItemStack out = instrument.copyWithCount(1);
        out.set(FDataComponents.CIRCUIT.get(), Circuit.of(circuit[0]));
        return out;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return FRecipes.INSTRUMENT_FIT.get();
    }
}
