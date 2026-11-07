// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.darkcell;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A plate that carries current through what stands on it. Real voltage and current; its harm is what it is fed. */
public class ShockPlateBlock extends Block implements EntityBlock, KillMethodBlock {

    public ShockPlateBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShockPlateBlockEntity(pos, state);
    }

    @Override
    public KillMethod method(Level level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof ShockPlateBlockEntity be ? be : null;
    }
}
