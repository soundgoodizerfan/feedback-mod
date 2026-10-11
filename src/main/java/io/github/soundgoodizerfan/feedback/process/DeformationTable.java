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

import java.util.List;
import java.util.Optional;

import net.minecraft.world.item.ItemStack;

/**
 * Every {@link Deformation}, as compiled from materials and form steps by {@link FormTable}.
 *
 * <h2>Why this is not a vanilla recipe type</h2>
 * Vanilla's recipe machinery brings a recipe book, a crafting-grid shaped matching API and
 * automatic client sync, none of which apply: there is no grid and nothing is ever "crafted". The
 * list is sent to the client by a payload of its own ({@code DeformationSyncPayload}) purely so
 * JEI can print what a material needs -- philosophy 8 gives requirements away -- under a category
 * whose catalysts are the machines that apply the operation, never a recipe list any of them owns.
 *
 * <h2>Nothing is authored here any more</h2>
 * This used to load {@code data/<ns>/deformation/*.json}, one file per recipe, each repeating its
 * material's hardness. The figures are now a function of hardness per form step; see
 * {@link Material} and {@link FormStep}.
 */
public final class DeformationTable {

    private static final DeformationTable INSTANCE = new DeformationTable();

    private DeformationTable() {}

    public static DeformationTable get() {
        return INSTANCE;
    }

    /** Every entry, in material then step order. Used to ship the table to the client for display. */
    public List<Deformation> entries() {
        return FormTable.get().deformations();
    }

    /** What this stack turns into under the given {@link Operation}, if anything does. */
    public Optional<Deformation> find(ItemStack stack, Operation operation) {
        for (Deformation entry : entries())
            if (entry.operation() == operation && entry.input().test(stack))
                return Optional.of(entry);
        return Optional.empty();
    }
}
