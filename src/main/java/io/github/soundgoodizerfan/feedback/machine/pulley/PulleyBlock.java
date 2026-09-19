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
package io.github.soundgoodizerfan.feedback.machine.pulley;

import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A wheel on a shaft, coupling locally exactly like a Cog -- the belt to a distant partner is
 * handled entirely inside {@link PulleyBlockEntity} and is invisible to {@code RotationPropagator}.
 * Two sizes, one block class, the same split {@code CogBlock} already uses.
 */
public class PulleyBlock extends RotatedPillarBlock implements EntityBlock, Rotatable {

    private final int radius;
    private final VoxelShape shapeX;
    private final VoxelShape shapeY;
    private final VoxelShape shapeZ;

    public PulleyBlock(Properties properties, int radius, int thickness) {
        super(properties);
        this.radius = radius;

        float half = thickness / 2f;
        float near = 8 - half;
        float far = 8 + half;
        float low = 8 - radius;
        float high = 8 + radius;
        this.shapeX = box(near, low, low, far, high, high);
        this.shapeY = box(low, near, low, high, far, high);
        this.shapeZ = box(low, low, near, high, high, far);
    }

    public int getRadius() {
        return radius;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> shapeX;
            case Y -> shapeY;
            case Z -> shapeZ;
        };
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        if (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown()) {
            BlockState against = context.getLevel()
                    .getBlockState(context.getClickedPos().relative(face.getOpposite()));
            if (against.getBlock() instanceof Rotatable rotatable)
                return defaultBlockState().setValue(AXIS, rotatable.getRotationAxis(against));
        }
        return defaultBlockState().setValue(AXIS, face.getAxis());
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    @Override
    public boolean hasShaftTowards(LevelAccessor level, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PulleyBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
                ? (l, p, s, be) -> ((PulleyBlockEntity) be).tickClient()
                : (l, p, s, be) -> ((PulleyBlockEntity) be).tickServer();
    }
}
