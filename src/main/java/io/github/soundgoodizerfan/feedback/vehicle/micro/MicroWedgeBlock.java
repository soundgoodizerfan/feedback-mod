// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A ramp for micro builds (vehicles v2 spec, <em>part shapes</em>): a block whose slope falls towards
 * {@link #FACING}, drawn and collided as four steps one sixteenth of a block wide, which is the finest
 * the box colliders resolve. As a voxel the slope is a quarter of a block long, so a wedge voxel is a
 * four-step ramp one voxel across; the same four boxes are the model, the player's outline and the
 * Rapier collider ({@link #steps}), so what you see is what collides. Full size it is a stair-like block.
 */
public class MicroWedgeBlock extends Block {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;

    /** Steps in a wedge: its length in voxel sixteenths. */
    public static final int STEPS = 4;

    private static final VoxelShape[] SHAPES = new VoxelShape[8];

    static {
        for (Direction f : Direction.Plane.HORIZONTAL)
            for (Half h : Half.values()) {
                VoxelShape s = Shapes.empty();
                for (int[] b : steps(f, h, 16))
                    s = Shapes.or(s, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
                SHAPES[f.get2DDataValue() * 2 + h.ordinal()] = s;
            }
    }

    public MicroWedgeBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(HALF, Half.BOTTOM));
    }

    /**
     * The four step boxes of a wedge as {@code [x0, y0, z0, x1, y1, z1]}, in units where the whole
     * block is {@code size} across: 16 for the model and full-size shape, 4 for one voxel's sixteenths.
     * Step k starts at the high end (opposite the facing) and is {@code size/4} long, a sixteenth less
     * high each step; a top wedge hangs from the ceiling.
     */
    public static int[][] steps(Direction facing, Half half, int size) {
        int[][] out = new int[STEPS][];
        int len = size / STEPS;
        for (int k = 0; k < STEPS; k++) {
            int along0 = k * len, along1 = along0 + len;
            // How far up (or down from the top) this step reaches.
            int high = size - k * len;
            int y0 = half == Half.BOTTOM ? 0 : size - high, y1 = half == Half.BOTTOM ? high : size;
            out[k] = switch (facing) {
                case EAST -> new int[]{along0, y0, 0, along1, y1, size};
                case WEST -> new int[]{size - along1, y0, 0, size - along0, y1, size};
                case SOUTH -> new int[]{0, y0, along0, size, y1, along1};
                default -> new int[]{0, y0, size - along1, size, y1, size - along0};
            };
        }
        return out;
    }

    public static boolean is(BlockState state) {
        return state.getBlock() instanceof MicroWedgeBlock;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF);
    }

    /** The slope falls away from the player, on the half of the face that was clicked. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean top = context.getClickedFace() == Direction.DOWN
                || context.getClickedFace() != Direction.UP && context.getClickLocation().y - context.getClickedPos().getY() > 0.5;
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite()).setValue(HALF, top ? Half.TOP : Half.BOTTOM);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get2DDataValue() * 2 + state.getValue(HALF).ordinal()];
    }
}
