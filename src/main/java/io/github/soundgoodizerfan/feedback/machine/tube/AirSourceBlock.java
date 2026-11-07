// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.tube;

import java.util.function.BiFunction;

import io.github.soundgoodizerfan.feedback.machine.creative.CreativeSourceBlock;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** The creative air source: air at any pressure into a tube run, the test supply for pneumatic transport. */
public class AirSourceBlock extends CreativeSourceBlock {

    public AirSourceBlock(Properties properties, BiFunction<BlockPos, BlockState, BlockEntity> entity) {
        super(properties, entity);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((AirSourceBlockEntity) be).tickServer();
    }
}
