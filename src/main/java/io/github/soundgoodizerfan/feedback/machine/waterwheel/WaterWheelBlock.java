// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.waterwheel;

import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Continuous rotation, paid for with real estate and a river rather than with fuel.
 * <p>
 * Slice 1 gives it 256 Su against the hammer's 80, which is the arithmetic the player is meant
 * to run: three hammers fit, four do not, and nothing anywhere says "requires tier 2".
 */
public class WaterWheelBlock extends RotatedPillarBlock implements EntityBlock, Rotatable {

    public WaterWheelBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, context.getClickedFace().getAxis());
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public boolean hasShaftTowards(LevelAccessor level, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
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
        return new WaterWheelBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? (l, p, s, be) -> ((WaterWheelBlockEntity) be).tickClient()
                : (l, p, s, be) -> ((WaterWheelBlockEntity) be).tickServer();
    }
}
