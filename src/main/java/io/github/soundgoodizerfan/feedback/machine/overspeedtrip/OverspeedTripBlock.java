// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.overspeedtrip;

import io.github.soundgoodizerfan.feedback.machine.clutch.ClutchBlock;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A clutch that lets go by itself. Same shaft coupling, same {@code ENGAGED} state, same hand
 * and controller reset -- see {@link OverspeedTripBlockEntity} for the one thing it adds.
 */
public class OverspeedTripBlock extends ClutchBlock {

    public OverspeedTripBlock(Properties properties) {
        super(properties);
    }

    /** Sneak and right-click swaps the flyweight spring; a plain click throws it like a clutch. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!player.isShiftKeyDown())
            return super.useWithoutItem(state, level, pos, player, hit);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof OverspeedTripBlockEntity trip)
            player.displayClientMessage(Component.translatable("message.feedback.overspeed_trip.spring",
                    Math.round(trip.cycleSpring())), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OverspeedTripBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? (l, p, s, be) -> ((OverspeedTripBlockEntity) be).tickClient()
                : (l, p, s, be) -> ((OverspeedTripBlockEntity) be).tickServer();
    }
}
