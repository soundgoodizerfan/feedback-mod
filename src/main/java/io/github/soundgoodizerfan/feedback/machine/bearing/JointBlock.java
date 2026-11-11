// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.bearing;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A ball, locked, slider or piston joint (vehicles v2 spec, micro joints). Like the Rotary Bearing it
 * is a face add-on: it points away from the face it was put against, and as a micro voxel an empty
 * hand assembles or puts back what is in front ({@code MicroBearings}). It has no block entity: its
 * state is the voxel's, and the hold lives in the cell ({@link Joint}). {@link #VARIANT} is the
 * named choice: the ball's cone (narrow, stock, wide) or the slider's spring (soft, stock, stiff);
 * sneaking with an empty hand cycles it while nothing is assembled.
 */
public class JointBlock extends Block implements JointPart {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 2);

    private static final VoxelShape[] SHAPES = new VoxelShape[6];

    static {
        for (Direction f : Direction.values())
            SHAPES[f.ordinal()] = Shapes.or(RotaryBearingBlock.facingBox(f, 2, 0, 2, 14, 9, 14),
                    RotaryBearingBlock.facingBox(f, 5, 9, 5, 11, 15, 11));
    }

    private final Joint.Kind kind;

    public JointBlock(Joint.Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.UP).setValue(VARIANT, 1));
    }

    @Override
    public Joint.Kind kind() {
        return kind;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, VARIANT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getClickedFace());
        // A slider wound of a metal rides in the band that metal's yield strength gives; the joint is not made of it afterwards.
        var metal = context.getItemInHand().get(io.github.soundgoodizerfan.feedback.registry.FDataComponents.SLIDER_SPRING.get());
        return kind == Joint.Kind.SLIDER && metal != null ? state.setValue(VARIANT, SliderSpringRecipe.band(metal)) : state;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).ordinal()];
    }

    /** The next named choice, wrapping: what a sneak-click with an empty hand does to an unassembled joint. */
    public static BlockState cycled(BlockState state) {
        return state.setValue(VARIANT, (state.getValue(VARIANT) + 1) % 3);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide)
            return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown())
            level.setBlock(pos, cycled(state), Block.UPDATE_ALL);
        else
            player.displayClientMessage(Component.translatable("message.feedback.joint.micro_only"), true);
        return InteractionResult.CONSUME;
    }
}
