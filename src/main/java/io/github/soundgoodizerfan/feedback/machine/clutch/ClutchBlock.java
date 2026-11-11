// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.clutch;

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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A shaft coupling that can be let go.
 *
 * <h2>Stopping is its own problem</h2>
 * Philosophy 13 separates sensing from stopping, and gives each energy type its own stopping
 * hardware with its own failure character. For rotation that hardware is a clutch, and its
 * character is that <strong>it does not stop anything</strong> — it stops <em>driving</em>.
 * Whatever was downstream keeps turning on its own momentum until friction takes it, which with
 * a flywheel on the line can be a very long time.
 * <p>
 * That falls out of machinery already built rather than being written here: disengaging splits
 * the run into two networks, and the far one simply has no source. The inertia model does the
 * rest.
 */
public class ClutchBlock extends Block implements EntityBlock, io.github.soundgoodizerfan.feedback.core.rotation.MicroRotation {

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty ENGAGED = BooleanProperty.create("engaged");

    public ClutchBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(FACING, Direction.NORTH)
                .setValue(ENGAGED, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ENGAGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }

    /** Right click throws the clutch by hand. A player is a perfectly good controller (§13). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ClutchBlockEntity clutch)
            clutch.setEngaged(!clutch.isEngaged());
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public io.github.soundgoodizerfan.feedback.core.unit.Drag getDragSuPerRpm() {
        return io.github.soundgoodizerfan.feedback.core.FTuning.SHAFT_DRAG_SU_PER_RPM;
    }

    @Override
    public io.github.soundgoodizerfan.feedback.core.unit.Inertia getInertia() {
        return io.github.soundgoodizerfan.feedback.core.FTuning.SHAFT_INERTIA;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    /**
     * Coupled to the driving side always, and to the driven side only while engaged.
     * <p>
     * Deliberately asymmetric. A disengaged clutch is still bolted to the shaft that turns it —
     * it is the output that lets go — so it stays a member of the driving network and keeps
     * paying its friction there. Cutting both faces would make the clutch itself vanish from the
     * network it is plainly still attached to.
     */
    @Override
    public boolean hasShaftTowards(LevelAccessor level, BlockPos pos, BlockState state, Direction face) {
        Direction output = state.getValue(FACING);
        if (face == output.getOpposite())
            return true;
        return face == output && state.getValue(ENGAGED);
    }

    /**
     * Kept out of the chunk mesh, because {@code RotatingVisual} draws this block itself and would
     * otherwise put a turning copy on top of a motionless one. With Flywheel's backend off,
     * {@code RotatingRenderer} draws it instead; there is no path where nothing does.
     * <p>
     * {@code ENTITYBLOCK_ANIMATED} rather than {@code INVISIBLE}: the chunk mesh skips both, but
     * only {@code INVISIBLE} also suppresses block-breaking particles, and a shaft that shatters
     * silently is a sense taken away for nothing.
     */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ClutchBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // The clutch does its own work on right click and needs no server tick. The client tick is
        // purely the rendered angle -- without it a clutch sits frozen in the middle of a run that
        // is plainly turning, which reads as the clutch being disengaged when it is not.
        return level.isClientSide ? (l, p, s, be) -> ((ClutchBlockEntity) be).tickClient() : null;
    }
}
