// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/**
 * The dosimeter: reads the dose the carrier's body has taken, in Ru x s, to the
 * nearest {@link ItemTuning#DOSIMETER_RESOLUTION}. Not the rate -- that is the Geiger counter's -- and
 * not the badge's own: it reads what got through the armour, which is the figure the body's three
 * illness steps are set against. It is passive; the figure shows on the hand card when held.
 */
public class DosimeterItem extends Item implements Instrument {

    public DosimeterItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.DOSE;
    }

    @Override
    public float resolution(Quantity quantity) {
        return ItemTuning.DOSIMETER_RESOLUTION;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.dosimeter");
    }
}
