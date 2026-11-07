// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.fitting.sensor.CurrentSensorFitting;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Bolts a {@link CurrentSensorFitting} onto the clicked face of a {@link Fittable} electrical
 * node. The same shape as {@link TemperatureSensorItem}, and for the same reason it runs in
 * {@code onItemUseFirst}: attaching a sensor must never be mistaken for using the block.
 */
public class CurrentSensorItem extends Item {

    public CurrentSensorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (!(be instanceof Fittable fittable) || !(be instanceof ElectricNode node))
            return InteractionResult.PASS;

        Direction side = context.getClickedFace();
        if (fittable.hasSidedFitting(side))
            return InteractionResult.PASS;

        CurrentSensorFitting fitting = new CurrentSensorFitting(node, side);
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
