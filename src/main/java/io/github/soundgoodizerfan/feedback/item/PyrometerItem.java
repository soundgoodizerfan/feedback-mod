// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * A lens that gathers a hot thing's glow onto a sensor, so a vessel can be read from across the
 * room instead of with a probe through the door. It reads the temperature of whatever
 * {@link ThermalBody} it is aimed at, sealed or not, finer than the thermometer; the price is
 * that it needs a lens and a clear line of sight.
 */
public class PyrometerItem extends Item implements Instrument {

    public PyrometerItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean canRead(Quantity quantity) {
        return quantity == Quantity.TEMPERATURE;
    }

    @Override
    public float resolution(Quantity quantity) {
        return ItemTuning.PYROMETER_RESOLUTION_TU.value();
    }

    @Override
    public Component label() {
        return Component.translatable("feedback.instrument.pyrometer");
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide)
            return InteractionResultHolder.success(stack);
        var from = player.getEyePosition();
        var to = from.add(player.getLookAngle().scale(ItemTuning.PYROMETER_RANGE_BLOCKS));
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.BLOCK) {
            Float known = Instruments.reference(level, hit.getBlockPos(), hit.getDirection(), Quantity.TEMPERATURE);
            if (known != null) {
                Instruments.recalibrate(stack, Quantity.TEMPERATURE);
                Instrument fresh = Instruments.fitted(stack, this);
                float shown = fresh.shown(Quantity.TEMPERATURE, known);
                io.github.soundgoodizerfan.feedback.net.AimPayloads.dial(player, hit.getBlockPos(), fresh, Component.translatable("feedback.readout.tu", Math.round(shown)),
                        Quantity.TEMPERATURE, shown);
                return InteractionResultHolder.success(stack);
            }
        }
        if (hit.getType() == HitResult.Type.BLOCK && level.getBlockEntity(hit.getBlockPos()) instanceof ThermalBody body) {
            Instrument self = Instruments.fitted(stack, this);
            float shown = self.shown(Quantity.TEMPERATURE, body.getTemperature().value());
            io.github.soundgoodizerfan.feedback.net.AimPayloads.dial(player, hit.getBlockPos(), self, Component.translatable("feedback.readout.tu", Math.round(shown)),
                    Quantity.TEMPERATURE, shown);
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }
}
