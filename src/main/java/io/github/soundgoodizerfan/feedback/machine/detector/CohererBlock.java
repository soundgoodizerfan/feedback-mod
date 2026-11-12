// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.detector;

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
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;

/** A redstone coherer. Empty-hand click is the tap. See {@link CohererBlockEntity}. */
public class CohererBlock extends Block implements EntityBlock {

    public static final BooleanProperty WELDED = BooleanProperty.create("welded");

    public CohererBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(WELDED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WELDED);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CohererBlockEntity coherer) {
            boolean welded = coherer.isEngaged();
            io.github.soundgoodizerfan.feedback.net.AimPayloads.report(player, pos, Component.translatable(!welded ? "feedback.coherer.loose"
                    : coherer.tap() ? "feedback.coherer.tapped" : "feedback.coherer.stuck"));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CohererBlockEntity(pos, state);
    }
}
