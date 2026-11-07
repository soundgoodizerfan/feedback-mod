// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.creative;

import java.util.function.BiFunction;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The block of a creative source. Using it opens its settings screen, for operators only. */
public class CreativeSourceBlock extends Block implements EntityBlock {

    private final BiFunction<BlockPos, BlockState, BlockEntity> entity;

    public CreativeSourceBlock(Properties properties, BiFunction<BlockPos, BlockState, BlockEntity> entity) {
        super(properties);
        this.entity = entity;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!player.canUseGameMasterBlocks())
            return InteractionResult.PASS;
        if (level.isClientSide)
            io.github.soundgoodizerfan.feedback.client.CreativeSourceScreen.open(pos);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return entity.apply(pos, state);
    }
}
