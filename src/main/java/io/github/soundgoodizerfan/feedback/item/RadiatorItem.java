// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.fitting.RadiatorFitting;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineBlockEntity;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Fits a {@link RadiatorFitting} onto the clicked face of an engine, ahead of the engine's own click handling ({@link StarterItem}). */
public class RadiatorItem extends Item {

    public RadiatorItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        Level level = context.getLevel();
        BlockEntity be = level.getBlockEntity(context.getClickedPos());
        if (!(be instanceof EngineBlockEntity engine))
            return InteractionResult.PASS;
        Direction side = context.getClickedFace();
        if (engine.hasSidedFitting(side))
            return InteractionResult.PASS;
        RadiatorFitting fitting = new RadiatorFitting();
        if (!engine.canMount(fitting, side))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            engine.setSidedFitting(side, fitting);
            fitting.onAttached();
            Player player = context.getPlayer();
            if (player != null && !player.getAbilities().instabuild)
                stack.shrink(1);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
