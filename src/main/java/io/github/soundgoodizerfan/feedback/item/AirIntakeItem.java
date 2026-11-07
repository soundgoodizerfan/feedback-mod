// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.fitting.upgrade.AirIntakeFitting;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Fits a {@link AirIntakeFitting} into whichever {@link Fittable} was clicked that says it
 * will honour one -- the gas compressor does, once. Unsided, like the refractory lining; the holder
 * decides, nothing here knows what a compressor is.
 */
public class AirIntakeItem extends Item {

    public AirIntakeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (!(be instanceof Fittable fittable))
            return InteractionResult.PASS;

        AirIntakeFitting fitting = new AirIntakeFitting();
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
