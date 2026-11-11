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
package io.github.soundgoodizerfan.feedback.machine.bearing;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.MicroRotation;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A bearing as a face add-on ({@code docs/specs/feedback_micro_spec.md}, <em>Bearings and ring gears</em>):
 * placed against a rotation block's face, its back couples to that block as a shaft end does, and
 * its {@link #FACING} is the turning face. Right-click with an empty hand assembles what is in front
 * into a Sable body hinged here and turned by the run ({@link RotaryBearingBlockEntity}); again puts
 * it back as blocks. Create's mechanical bearing in behaviour; no code taken. Also a micro voxel
 * ({@code MicroRotation}): then it turns voxels ({@code vehicle/micro/MicroBearings}).
 */
public class RotaryBearingBlock extends Block implements EntityBlock, MicroRotation {

    /** The turning face: where the assembled body sits. */
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private static final VoxelShape[] SHAPES = new VoxelShape[6];

    static {
        for (Direction f : Direction.values())
            SHAPES[f.ordinal()] = net.minecraft.world.phys.shapes.Shapes.or(
                    facingBox(f, 2, 0, 2, 14, 10, 14), facingBox(f, 1, 10, 1, 15, 15.5, 15));
    }

    public RotaryBearingBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP));
    }

    /**
     * A box given for a block facing up (sixteenths), turned to face {@code f}: y runs from the back
     * to the front along {@code f}, x and z fill the other two axes (the boxes used are square across).
     */
    public static VoxelShape facingBox(Direction f, double x0, double y0, double z0, double x1, double y1, double z1) {
        boolean positive = f.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        double a0 = positive ? y0 : 16 - y1, a1 = positive ? y1 : 16 - y0;
        return switch (f.getAxis()) {
            case Y -> box(x0, a0, z0, x1, a1, z1);
            case X -> box(a0, x0, z0, a1, x1, z1);
            case Z -> box(x0, z0, a0, x1, z1, a1);
        };
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** On the face clicked, pointing away from it: a face add-on to the block it was put against. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).ordinal()];
    }

    @Override
    public Drag getDragSuPerRpm() {
        return FTuning.ROTARY_BEARING_DRAG_SU_PER_RPM;
    }

    /** The housing's own; an assembled body's inertia is added by the bearing's hold. */
    @Override
    public Inertia getInertia() {
        return FTuning.ROTARY_BEARING_INERTIA;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    /** The back alone: a shaft end into the host. */
    @Override
    public boolean hasShaftTowards(LevelAccessor level, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING).getOpposite();
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        if (level instanceof ServerLevel server && level.getBlockEntity(pos) instanceof RotaryBearingBlockEntity be)
            player.displayClientMessage(Component.translatable("message.feedback.rotary_bearing." + be.toggle(server)), true);
        return InteractionResult.CONSUME;
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RotaryBearingBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : (l, p, s, be) -> ((RotaryBearingBlockEntity) be).tickServer((ServerLevel) l);
    }
}
