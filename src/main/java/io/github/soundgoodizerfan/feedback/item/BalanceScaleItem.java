// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/**
 * Puts a figure on how heavy a made thing is. Carried, like the calipers: owning it is enough to
 * read the mass of any modular item on its tooltip ({@code client/ModularReadings}). The tooltip
 * says "head-heavy" for free; how many kilograms of head needs the scale (spec 6). Calipers are
 * left to dimensions and condition.
 */
public class BalanceScaleItem extends Item implements Instrument {

    public BalanceScaleItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.MASS;
    }

    @Override
    public float resolution(Quantity quantity) {
        return ModularTuning.BALANCE_RESOLUTION;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.balance_scale");
    }
}
