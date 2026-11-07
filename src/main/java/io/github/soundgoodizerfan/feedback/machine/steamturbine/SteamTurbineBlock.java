// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.steamturbine;

import io.github.soundgoodizerfan.feedback.machine.steamengine.SteamEngineBlock;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The Steam Engine's block (facing, shaft ends, ticker) with the turbine's block entity. */
public class SteamTurbineBlock extends SteamEngineBlock {

    public SteamTurbineBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SteamTurbineBlockEntity(pos, state);
    }
}
