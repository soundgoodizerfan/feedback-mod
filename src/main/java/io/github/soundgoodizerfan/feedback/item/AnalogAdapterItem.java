// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.control.Analog;
import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.fitting.RedstoneAnalogFitting;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Fits a {@link RedstoneAnalogFitting} on the clicked face of a {@link Fittable} machine that has a variable setting. */
public class AnalogAdapterItem extends Item {

    public AnalogAdapterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (!(be instanceof Fittable fittable) || !(be instanceof Analog))
            return InteractionResult.PASS;
        Direction side = context.getClickedFace();
        if (fittable.hasSidedFitting(side))
            return InteractionResult.PASS;
        RedstoneAnalogFitting fitting = new RedstoneAnalogFitting(be, side);
        if (!fittable.canMount(fitting, side))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            fittable.setSidedFitting(side, fitting);
            fitting.onAttached();
            be.setChanged();
            RedstoneAnalogFitting.apply(level, context.getClickedPos());
            Player player = context.getPlayer();
            if (player != null && !player.getAbilities().instabuild)
                stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
