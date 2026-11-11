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
package io.github.soundgoodizerfan.feedback.machine.adapter;

import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import io.github.soundgoodizerfan.feedback.machine.bearing.RotaryBearingBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

/**
 * The one joint between the full-size rotation world and the micro one
 * ({@code docs/specs/feedback_micro_spec.md}, <em>Micro rotation nodes</em>). Its back, the big face,
 * couples to full-size rotation blocks as a shaft end does; its {@link #FACING}, the micro face,
 * reaches only the four voxels around the centre of the micro cell's face in front of it -- the
 * grid is four on a side, so no voxel is dead centre -- not through the propagator but by feeding each of
 * their runs at the adapter's speed ({@code RotationAdapterBlockEntity#tickServer}), and the other way when
 * a micro run in front is fed from elsewhere. {@link #hasShaftTowards} answers for the big face alone, so
 * no full-size block couples to the micro face.
 *
 * <p>Pure transmission: no source, no load, no inventory. Not itself a voxel: it is the boundary.
 */
public class RotationAdapterBlock extends Block implements EntityBlock, Rotatable {

    /** The micro face: where the four quarter shafts come out. */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    /** The housing, the big stub at the back, the four small at the front; built for each facing. */
    private static final VoxelShape[] SHAPES = new VoxelShape[6];

    static {
        for (Direction f : Direction.values())
            SHAPES[f.ordinal()] = Shapes.or(RotaryBearingBlock.facingBox(f, 2, 2, 2, 14, 14, 14),
                    RotaryBearingBlock.facingBox(f, 6, 0, 6, 10, 2, 10),
                    RotaryBearingBlock.facingBox(f, 5.5, 14, 5.5, 6.5, 16, 6.5),
                    RotaryBearingBlock.facingBox(f, 9.5, 14, 5.5, 10.5, 16, 6.5),
                    RotaryBearingBlock.facingBox(f, 5.5, 14, 9.5, 6.5, 16, 10.5),
                    RotaryBearingBlock.facingBox(f, 9.5, 14, 9.5, 10.5, 16, 10.5));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).ordinal()];
    }

    public RotationAdapterBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The micro face points the way the player looks: place it looking at the cell. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection());
    }

    /** The face that couples to a micro cell, or null if {@code state} is not an adapter. */
    @Nullable
    public static Direction microFace(BlockState state) {
        return state.getBlock() instanceof RotationAdapterBlock ? state.getValue(FACING) : null;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelAccessor level, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    /** Drawn by {@code client/SpinningParts}: the housing stands, the shafts turn. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? (l, p, s, be) -> ((io.github.soundgoodizerfan.feedback.core.rotation.RotationNode) be).tickClient()
                : (l, p, s, be) -> ((RotationAdapterBlockEntity) be).tickServer();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RotationAdapterBlockEntity(pos, state);
    }
}
