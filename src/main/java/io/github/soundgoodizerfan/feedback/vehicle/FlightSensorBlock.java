// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle;

import com.mojang.serialization.MapCodec;

import io.github.soundgoodizerfan.feedback.registry.FVehicles;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * The flight sensor (vehicles spec, instruments): a block in a vehicle that reads its height, its
 * speed through the air or its tilt and feeds a Controller's Input card, so an autopilot is only
 * cards. Use steps which reading it takes (altitude, airspeed, pitch, roll); the facing is its front,
 * which pitch and roll are measured about. Model: placeholder cube, art not drawn.
 */
public class FlightSensorBlock extends BaseEntityBlock {

    public static final MapCodec<FlightSensorBlock> CODEC = simpleCodec(FlightSensorBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public FlightSensorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof FlightSensorBlockEntity sensor) {
            if (player.isShiftKeyDown()) {
                player.displayClientMessage(sensor.readValue(), true);
            } else {
                sensor.nextMode();
                player.displayClientMessage(Component.translatable("message.feedback.flight_sensor.mode",
                        Component.translatable("feedback.flight_sensor." + sensor.mode().getSerializedName())), true);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FlightSensorBlockEntity(pos, state);
    }
}
