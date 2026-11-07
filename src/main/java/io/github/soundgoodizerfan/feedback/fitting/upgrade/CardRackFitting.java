// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.fitting.upgrade;

import io.github.soundgoodizerfan.feedback.fitting.UpgradeFitting;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.world.item.ItemStack;

/**
 * A rack of card guides fitted to a Controller: {@code FTuning.CARD_RACK_NODES} more cards a
 * program may hold per rack, up to {@code FTuning.CARD_RACK_MAX}. Stateless; the holder persists a count.
 */
public class CardRackFitting implements UpgradeFitting {

    @Override
    public ItemStack getPickItem() {
        return new ItemStack(FItems.CARD_RACK.get());
    }
}
