// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.converter;

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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import org.jetbrains.annotations.Nullable;

/** The input side of a solid-state converter: an empty hand picks the next ratio. See {@link ConverterBlockEntity}. */
public class ConverterBlock extends Block implements EntityBlock {

    public static final EnumProperty<Ratio> RATIO = EnumProperty.create("ratio", Ratio.class);

    public ConverterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(RATIO, Ratio.DIV4));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(RATIO);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        Ratio next = state.getValue(RATIO).next();
        level.setBlock(pos, state.setValue(RATIO, next), Block.UPDATE_ALL);
        player.displayClientMessage(Component.translatable("message.feedback.converter_ratio", Component.translatable("converter.feedback.ratio." + next.getSerializedName())), true);
        return InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ConverterBlockEntity(pos, state);
    }
}
