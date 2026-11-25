// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.fitting.StarterFitting;
import io.github.soundgoodizerfan.feedback.vehicle.engine.EngineBlockEntity;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Bolts a {@link StarterFitting} onto the clicked face of an engine. Runs ahead of the engine's own click handling
 * ({@code onItemUseFirst}), as {@link TemperatureSensorItem} does, so fitting it is never taken for toggling the engine.
 */
public class StarterItem extends Item {

    public StarterItem(Properties properties) {
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
        StarterFitting fitting = new StarterFitting(be, side);
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
