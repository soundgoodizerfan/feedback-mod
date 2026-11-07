// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import java.util.List;
import java.util.Optional;

import net.minecraft.world.item.ItemStack;

/**
 * Every {@link Removal}, as compiled from materials and form steps by {@link FormTable} -- shaped
 * like {@link DeformationTable}, and for the same reason not a vanilla {@code RecipeType}. One
 * table serves the Drill Press, the Lathe and the Grinding Wheel, but {@link #find} matches on
 * {@link Operation} as well as the input item -- see {@link Operation}'s class doc for why a
 * shared table and a shared operation are not the same claim.
 */
public final class RemovalTable {

    private static final RemovalTable INSTANCE = new RemovalTable();

    private RemovalTable() {}

    public static RemovalTable get() {
        return INSTANCE;
    }

    public List<Removal> entries() {
        return FormTable.get().removals();
    }

    /** What this stack becomes under the given {@link Operation}, if anything does. */
    public Optional<Removal> find(ItemStack stack, Operation operation) {
        for (Removal entry : entries())
            if (entry.operation() == operation && entry.input().test(stack) && entry.applies(stack))
                return Optional.of(entry);
        return Optional.empty();
    }
}
