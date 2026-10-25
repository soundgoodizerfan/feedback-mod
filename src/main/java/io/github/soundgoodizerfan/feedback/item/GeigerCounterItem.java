/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
