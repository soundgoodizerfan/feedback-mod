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
 * The dosimeter (mechanics 9.8): reads the dose the carrier's body has taken, in Ru x s, to the
 * nearest {@link FTuning#DOSIMETER_RESOLUTION}. Not the rate -- that is the Geiger counter's -- and
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
        return FTuning.DOSIMETER_RESOLUTION;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.dosimeter");
    }
}
