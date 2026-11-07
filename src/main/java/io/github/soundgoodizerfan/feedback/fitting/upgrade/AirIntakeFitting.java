// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.fitting.upgrade;

import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.world.item.ItemStack;

/**
 * A screened air intake fitted to a gas compressor: with it, a compressor that finds no gas behind it draws the
 * air around it instead, as much as the dimension's air density allows. Stateless; the compressor does the drawing
 * and persists a flag.
 */
public class AirIntakeFitting implements UpgradeFitting {

    @Override
    public ItemStack getPickItem() {
        return new ItemStack(FItems.AIR_INTAKE.get());
    }
}
