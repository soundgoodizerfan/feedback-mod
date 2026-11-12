// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A Counter: goods go in from a hopper or belt, a villager nearby is handed them, emeralds and returned goods come out of other slots. */
public class CounterBlock extends Block implements EntityBlock {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    public CounterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The front, the customer side, faces the player who places it. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** The face clicked decides the menu: front or top buys, back keeps the shop, a side reads the catchment. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof CounterBlockEntity counter) || !(player instanceof ServerPlayer sp))
            return InteractionResult.PASS;
        Direction face = hit.getDirection();
        Direction front = state.getValue(FACING);
        counter.touch();
        if (face == front || face == Direction.UP) {
            open(sp, counter, CounterMenu.BUY, "feedback.counter.buy");
        } else if (face == front.getOpposite()) {
            if (counter.villageOwned()) {
                player.displayClientMessage(Component.translatable("feedback.counter.village_keeps"), true);
                return InteractionResult.CONSUME;
            }
            counter.keeper(player.getUUID());
            open(sp, counter, CounterMenu.KEEPER, "feedback.counter.keeper");
        } else {
            io.github.soundgoodizerfan.feedback.net.AimPayloads.report(player, pos, counter.report());
        }
        return InteractionResult.CONSUME;
    }

    private static void open(ServerPlayer player, CounterBlockEntity counter, int mode, String title) {
        BlockPos pos = counter.getBlockPos();
        player.openMenu(new SimpleMenuProvider((id, inv, p) -> new CounterMenu(id, inv, mode, pos), Component.translatable(title)), buf -> {
            buf.writeVarInt(mode);
            buf.writeBlockPos(pos);
        });
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CounterBlockEntity counter)
            counter.dropAll();
        super.onRemove(state, level, pos, newState, moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CounterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((CounterBlockEntity) be).tickServer();
    }
}
