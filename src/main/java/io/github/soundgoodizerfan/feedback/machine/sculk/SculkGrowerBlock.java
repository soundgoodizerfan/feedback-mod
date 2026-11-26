// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.sculk;

import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** The Sculk Grower: a node on the XP run that harvests a ripe Culture Dish beside it. See {@link SculkGrowerBlockEntity}. */
public class SculkGrowerBlock extends Block implements EntityBlock {

    public SculkGrowerBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SculkGrowerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide || type != FBlockEntities.SCULK_GROWER.get() ? null
                : (l, p, s, be) -> ((SculkGrowerBlockEntity) be).tickServer();
    }
}
