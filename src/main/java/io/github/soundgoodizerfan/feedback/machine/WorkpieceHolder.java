// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine;

import net.minecraft.world.item.ItemStack;

/** A machine with one workpiece slot -- the Hammer and every {@link ShopMachineBlockEntity}. */
public interface WorkpieceHolder {

    ItemStack getWorkpiece();

    /** Takes one item off {@code stack} if the slot is clear. */
    boolean insert(ItemStack stack);

    /** Empties the slot; what comes out carries its own heat. */
    ItemStack removeWorkpiece();
}
