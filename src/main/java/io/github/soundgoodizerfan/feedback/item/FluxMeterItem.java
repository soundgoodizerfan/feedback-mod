// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.machine.reactor.ReactorCoreBlockEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Reads a reactor core's neutron flux, as a fraction of full power: the chain reaction itself, ahead of the heat it makes.
 * Use it on the core. Read-only, like every instrument.
 */
public class FluxMeterItem extends Item implements Instrument {

    public FluxMeterItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.FLUX;
    }

    @Override
    public float resolution(Quantity quantity) {
        return FTuning.FLUX_METER_RESOLUTION;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.flux_meter");
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos()) instanceof ReactorCoreBlockEntity core))
            return InteractionResult.PASS;
        if (!context.getLevel().isClientSide && context.getPlayer() != null) {
            Instrument self = Instruments.fitted(stack, this);
            context.getPlayer().displayClientMessage(Instruments.signed(self, Component.translatable("feedback.flux.reading",
                    Instruments.format(core.power(), self.resolution(Quantity.FLUX)))), true);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }
}
