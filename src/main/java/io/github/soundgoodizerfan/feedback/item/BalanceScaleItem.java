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
