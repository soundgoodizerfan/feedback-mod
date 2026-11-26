// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/**
 * Reads a length by counting light fringes: a split beam, one arm of it reflected off the thing measured, the two brought together again.
 * It is the calipers' finer cousin and reads only length: a carried interferometer is the best length instrument the displays find, so a part's
 * dimensions print to {@link ItemTuning#INTERFEROMETER_RESOLUTION_MM} where calipers stop at their coarser step. Like any instrument it
 * unlocks nothing; it buys significant figures (roadmap O5: a part cut to a tolerance the shop could not hold, because it can finally be
 * read).
 */
public class InterferometerItem extends Item implements Instrument {

    public InterferometerItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.LENGTH;
    }

    @Override
    public float resolution(Quantity quantity) {
        return quantity == Quantity.LENGTH ? ItemTuning.INTERFEROMETER_RESOLUTION_MM : 1f;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.interferometer");
    }
}
