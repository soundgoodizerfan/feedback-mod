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
import io.github.soundgoodizerfan.feedback.core.hazard.PressureHolder;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * A glass face, a copper plate and a bent tube: the thermometer's sibling for the other thing
 * that kills a boiler. Pressure is as invisible as temperature was, and the Boiler's danger line
 * is a cliff the player could not otherwise see coming.
 * <p>
 * Read-only by construction, like every instrument: {@link #onItemUseFirst} reads any {@link
 * PressureHolder} the player clicks and shows one action-bar line. It cannot vent, and it
 * does not lock anything; it only turns "the safety is about to matter" into a number.
 */
public class PressureGaugeItem extends Item implements Instrument {

    public PressureGaugeItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.PRESSURE;
    }

    @Override
    public float resolution(Quantity quantity) {
        return FTuning.PRESSURE_GAUGE_RESOLUTION_PU.value();
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.pressure_gauge");
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !(level.getBlockEntity(context.getClickedPos()) instanceof PressureHolder holder))
            return InteractionResult.PASS;

        if (!level.isClientSide) {
            Instrument self = Instruments.fitted(stack, this);
            player.displayClientMessage(Instruments.signed(self, Component.translatable("feedback.unit.pu",
                    Instruments.format(holder.getPressure().value(), self.resolution(Quantity.PRESSURE)))), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
