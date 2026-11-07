// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
