// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;

/**
 * A paper strip dipped in beetroot: it turns colour with the toxin in a food, in four shades. Carried like every
 * instrument, so a food's tooltip and the item-info HUD show the toxin band while one is in the pack; it reads
 * nothing else, and the world never consults it (a dish is as poisonous with or without one).
 */
public class TestStripItem extends Item implements Instrument {

    public TestStripItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.TOXIN;
    }

    @Override
    public float resolution(Quantity quantity) {
        return FTuning.TEST_STRIP_RESOLUTION_TOXIN;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.test_strip");
    }
}
