// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.belt;

import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The driven roller at a run's exit end. A shaft turning it across the belt is the only thing that
 * moves the run; every other cell is an idler. It carries cargo over itself like any cell, and the
 * cargo's mass is the load it asks of the shaft ({@link BeltHeadBlockEntity#getLoadSu}).
 */
public class BeltHeadBlock extends Block implements EntityBlock, BeltPart, Rotatable {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final net.minecraft.world.level.block.state.properties.BooleanProperty RUNNING = BeltBlock.RUNNING;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);

    public BeltHeadBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(RUNNING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, RUNNING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    public Direction beltFacing(BlockState state) {
        return state.getValue(FACING);
    }

    @Override
    public BeltBlock.Slope beltSlope(BlockState state) {
        return BeltBlock.Slope.FLAT;
    }

    /** The roller lies across the belt, so the shaft enters from either side. */
    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getClockWise().getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelAccessor level, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == getRotationAxis(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BeltHeadBlockEntity(pos, state);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
        super.onPlace(state, level, pos, old, moved);
        if (!level.isClientSide && !old.is(this))
            BeltRuns.of(level).queue(pos);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos from, boolean moved) {
        super.neighborChanged(state, level, pos, block, from, moved);
        if (!level.isClientSide && BeltRuns.adjacent(pos, from))
            BeltRuns.of(level).queue(pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        super.onRemove(state, level, pos, newState, moved);
        if (!level.isClientSide && !newState.is(this))
            BeltRuns.of(level).queue(pos);
    }

    @Override
    protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.phys.BlockHitResult hit) {
        if (level.isClientSide)
            return net.minecraft.world.InteractionResult.SUCCESS;
        return level.getBlockEntity(pos) instanceof BeltHeadBlockEntity head && head.handUse(player, hit)
                ? net.minecraft.world.InteractionResult.CONSUME : net.minecraft.world.InteractionResult.PASS;
    }

    @Override
    protected java.util.List<net.minecraft.world.item.ItemStack> getDrops(BlockState state, net.minecraft.world.level.storage.loot.LootParams.Builder params) {
        java.util.List<net.minecraft.world.item.ItemStack> out = new java.util.ArrayList<>(super.getDrops(state, params));
        if (params.getOptionalParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_ENTITY) instanceof BeltHeadBlockEntity head)
            head.moduleDrop().ifPresent(out::add);
        return out;
    }

    /** Carries what stands on it and takes dropped items, as a belt cell does. */
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.Entity entity) {
        ((BeltBlock) io.github.soundgoodizerfan.feedback.registry.FLogistics.BELT.get()).stepOn(level, pos, state.setValue(BeltBlock.FACING, state.getValue(FACING)), entity);
    }
}
