// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.fitting.upgrade;

import io.github.soundgoodizerfan.feedback.control.ControlTuning;
import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.world.item.ItemStack;

/**
 * A drum of remanent magnetic coating fitted to a Controller: {@code ControlTuning.MEMORY_DRUM_VARIABLES}
 * more variables per drum, up to {@code ControlTuning.MEMORY_DRUM_MAX}. Stateless; the holder persists a count.
 */
public class MemoryDrumFitting implements UpgradeFitting {

    @Override
    public ItemStack getPickItem() {
        return new ItemStack(FItems.MEMORY_DRUM.get());
    }
}
