/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.machine.linkage;

import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Turns rotation into strokes. Shaft goes in the back, machine sits in front.
 * <p>
 * This is a block the player places rather than a detail inside the hammer, because choosing the
 * throw is the slice's first real gearing decision and it should be a thing you can point at.
 */
public class CrankLinkageBlock extends Block implements EntityBlock, Rotatable {

    // A new linkage is the LONG throw -- the gentle one. A player who has not yet met the idea
    // of overrun should be able to make the thing they were aiming for; discovering that a
    // harder setting exists, is faster, and ruins the workpiece is a better order to learn in
    // than starting with a machine that only ever produces foil and no explanation.

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final EnumProperty<Throw> THROW = EnumProperty.create("throw", Throw.class);

    public CrankLinkageBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(THROW, Throw.LONG));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, THROW);
    }

    /**
     * Orient so the input end meets a shaft, if one is adjacent.
     * <p>
     * A linkage has a back that takes rotation and a front that pushes, and getting that
     * backwards silently produces a machine that does nothing -- there is no error, it simply
     * never turns. Rather than making the player reason about it, look for a shaft touching the
     * spot being built on and face away from it.
     */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        for (Direction face : context.getNearestLookingDirections()) {
            if (presentsShaft(context.getLevel(), pos.relative(face), face.getOpposite()))
                return defaultBlockState().setValue(FACING, face.getOpposite());
        }
        for (Direction face : Direction.values()) {
            if (presentsShaft(context.getLevel(), pos.relative(face), face.getOpposite()))
                return defaultBlockState().setValue(FACING, face.getOpposite());
        }
        // Nothing to couple to. Drive away from whatever was clicked, which is the usual intent.
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    /** Does the block at {@code pos} offer a shaft end pointing at {@code towards}? */
    private static boolean presentsShaft(LevelAccessor level, BlockPos pos, Direction towards) {
        BlockState state = level.getBlockState(pos);
        return state.getBlock() instanceof Rotatable rotatable
                && rotatable.hasShaftTowards(level, pos, state, towards);
    }

    /**
     * Right click swaps the throw. Re-gearing a machine you already own is the point of the
     * component, so it should not require breaking and replacing it.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide) {
            Throw next = state.getValue(THROW) == Throw.SHORT ? Throw.LONG : Throw.SHORT;
            level.setBlockAndUpdate(pos, state.setValue(THROW, next));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** The linkage's load is the machine in front of it, so placing or removing that machine
     * changes the network's ledger. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor,
                                   BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighbor, neighborPos, movedByPiston);
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof CrankLinkageBlockEntity linkage
                && linkage.getNetwork() != null)
            linkage.getNetwork().recalculate();
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelAccessor level, BlockPos pos, BlockState state, Direction face) {
        // Driven from behind only. The front is where the reciprocating machine goes.
        return face == state.getValue(FACING).getOpposite();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrankLinkageBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? (l, p, s, be) -> ((CrankLinkageBlockEntity) be).tickClient()
                : (l, p, s, be) -> ((CrankLinkageBlockEntity) be).tickServer();
    }
}
