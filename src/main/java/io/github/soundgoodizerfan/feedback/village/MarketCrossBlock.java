// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.BlockGetter;

/** A market cross: a pillar on a paved square. Right-click to read whether the square is big enough and when the next market is. */
public class MarketCrossBlock extends Block implements EntityBlock {

    private static final VoxelShape SHAPE = Block.box(5, 0, 5, 11, 16, 11);

    public MarketCrossBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MarketCrossBlockEntity cross) {
            long day = level.getGameTime() / VillageTuning.DAY_TICKS;
            int until = MarketDay.daysUntil(day);
            Component line = !cross.active() ? Component.translatable("village.feedback.cross.inactive")
                    : until == 0 ? Component.translatable("village.feedback.cross.today")
                    : Component.translatable("village.feedback.cross.in", until);
            io.github.soundgoodizerfan.feedback.net.AimPayloads.report(player, pos, line);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MarketCrossBlockEntity(pos, state);
    }
}
