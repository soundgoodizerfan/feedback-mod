// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
            io.github.soundgoodizerfan.feedback.net.AimPayloads.dial(player, context.getClickedPos(), self, Component.translatable("feedback.unit.pu",
                    Instruments.format(holder.getPressure().value(), self.resolution(Quantity.PRESSURE))), Quantity.PRESSURE,
                    Instruments.quantise(holder.getPressure().value(), self.resolution(Quantity.PRESSURE)));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
