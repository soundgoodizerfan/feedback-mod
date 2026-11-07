// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/**
 * The Geiger counter (mechanics 9.8): reads the dose rate in Ru, and says so out loud. It does
 * nothing in the hand but listen -- the clicks and the figure both come from {@code client/GeigerClient},
 * counted from the same random clicks, so the number is as steady as the clicks sound. It reads the
 * field at the player, before their armour: it tells you why to put lead on, not how much got through.
 */
public class GeigerCounterItem extends Item implements Instrument {

    public GeigerCounterItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.RADIATION;
    }

    @Override
    public float resolution(Quantity quantity) {
        return FTuning.GEIGER_RESOLUTION;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.geiger_counter");
    }
}
