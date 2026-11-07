// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.fitting.upgrade.ReliefValveFitting;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Fits a {@link ReliefValveFitting} into whichever {@link Fittable} was clicked that says it
 * will honour one -- the boiler does, once. Unsided, like the refractory lining; the holder
 * decides, nothing here knows what a boiler is.
 */
public class ReliefValveItem extends Item {

    public ReliefValveItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (!(be instanceof Fittable fittable))
            return InteractionResult.PASS;

        ReliefValveFitting fitting = new ReliefValveFitting();
        if (!fittable.canAdd(fitting))
            return InteractionResult.PASS;

        if (!level.isClientSide) {
            fittable.addUpgrade(fitting);
            fitting.onAttached();

            Player player = context.getPlayer();
            if (player != null && !player.getAbilities().instabuild)
                stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
