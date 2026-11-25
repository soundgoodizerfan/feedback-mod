// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/**
 * Puts figures on what a sample is made of. Carried, like the calipers and the balance scale: owning it is enough to read, on the tooltip of any
 * alloy, its constituents and their shares, and of any ore the metals it bears ({@code client/tooltip/SpectrumTooltip}). A prism spreads the
 * light a sample gives off or passes into lines; a photocell reads how strong each is. With Thaumaturge the same lines list the aspects of the
 * sample the reader has discovered.
 */
public class SpectrometerItem extends Item implements Instrument {

    public SpectrometerItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.COMPOSITION;
    }

    @Override
    public float resolution(Quantity quantity) {
        return ModularTuning.SPECTROMETER_RESOLUTION_PERCENT;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.spectrometer");
    }
}
