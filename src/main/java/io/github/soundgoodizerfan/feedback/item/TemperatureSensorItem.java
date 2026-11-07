// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.fitting.sensor.TemperatureSensorFitting;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Bolts a {@link TemperatureSensorFitting} onto whichever face of a {@link Fittable}
 * {@link ThermalBody} was clicked -- a Crucible, an Improved Furnace, or anything else that later
 * answers both. No identity check on the block: the same three lines fire whichever one is there.
 *
 * <h2>Why {@code onItemUseFirst} rather than {@code useOn}</h2>
 * Same reasoning as {@code CalipersItem} and {@code ThermometerItem}: this runs ahead of the
 * holder's own click handling, so attaching a sensor never gets mistaken for placing a workpiece.
 */
public class TemperatureSensorItem extends Item {

    public TemperatureSensorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (!(be instanceof Fittable fittable) || !(be instanceof ThermalBody thermal))
            return InteractionResult.PASS;

        Direction side = context.getClickedFace();
        if (fittable.hasSidedFitting(side))
            return InteractionResult.PASS;

        TemperatureSensorFitting fitting = new TemperatureSensorFitting(be, thermal, side);
        if (!fittable.canMount(fitting, side))
            return InteractionResult.PASS;

        if (!level.isClientSide) {
            fittable.setSidedFitting(side, fitting);
            fitting.onAttached();

            Player player = context.getPlayer();
            if (player != null && !player.getAbilities().instabuild)
                stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
