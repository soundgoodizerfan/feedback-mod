// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.fitting.upgrade;

import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.world.item.ItemStack;

/**
 * A toothed escape wheel fitted to a Controller: lets Delay cards run a typed tick count instead
 * of only the named lengths. One per Controller; stateless, the holder persists a flag.
 */
public class EscapementFitting implements UpgradeFitting {

    @Override
    public ItemStack getPickItem() {
        return new ItemStack(FItems.ESCAPEMENT.get());
    }
}
