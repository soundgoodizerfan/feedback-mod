// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * The full-size blocks of a micro body going back into the world: each plot block to the cell it was worked out to land in
 * (a turn about a voxel puts a block on the grid only at some turns, so the caller has the targets), its state turned with it.
 */
final class FullBlockTransform extends SubLevelAssemblyHelper.AssemblyTransform {

    private final Map<BlockPos, BlockPos> targets;
    private final UnaryOperator<BlockState> turn;

    FullBlockTransform(BlockPos homeFrom, BlockPos homeTo, ServerLevel level, Map<BlockPos, BlockPos> targets, UnaryOperator<BlockState> turn) {
        super(homeFrom, homeTo, 0, Rotation.NONE, level);
        this.targets = targets;
        this.turn = turn;
    }

    @Override
    public BlockPos apply(BlockPos p) {
        BlockPos to = targets.get(p);
        return to != null ? to : super.apply(p);
    }

    @Override
    public BlockState apply(BlockState state) {
        return turn.apply(super.apply(state));
    }
}
