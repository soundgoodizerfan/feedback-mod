// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.machine.battery.BatteryBlockEntity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Samples the acid of a battery: its density falls as the cells discharge, so the sample is the state of charge
 * ({@link Quantity#CHARGE}, a share of what the cells can hold). Use it on a battery. Read-only, to a step of
 * {@link ItemTuning#HYDROMETER_RESOLUTION} until a finer instrument narrows it.
 */
public class HydrometerItem extends Item implements Instrument {

    public HydrometerItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.CHARGE;
    }

    @Override
    public float resolution(Quantity quantity) {
        return ItemTuning.HYDROMETER_RESOLUTION;
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.hydrometer");
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !(level.getBlockEntity(context.getClickedPos()) instanceof BatteryBlockEntity battery))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            Instrument self = Instruments.fitted(stack, this);
            if (battery.ready())
                io.github.soundgoodizerfan.feedback.net.AimPayloads.dial(player, context.getClickedPos(), self, Component.translatable("feedback.battery.charge",
                        Instruments.format(battery.stateOfCharge() * 100f, self.resolution(Quantity.CHARGE) * 100f)), Quantity.CHARGE,
                        Instruments.quantise(battery.stateOfCharge(), self.resolution(Quantity.CHARGE)));
            else
                io.github.soundgoodizerfan.feedback.net.AimPayloads.read(player, context.getClickedPos(), self, Component.translatable("feedback.battery.dry"));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
