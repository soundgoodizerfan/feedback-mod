// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.cog;

import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;

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
 * A toothed wheel. Trades speed for force, or force for speed, and nothing else.
 *
 * <h2>Why gearing matters here and not only as a speed knob</h2>
 * Gearing down multiplies the force behind each blow and divides how often blows fall, so work
 * per second is unchanged -- what changes is whether a single blow clears a material's hardness
 * floor at all, which is the difference between working the material and doing nothing to it.
 * That makes a gear train the <em>second</em> answer to the force problem philosophy §5 asks for:
 * a short crank throw is free but spends precision, since the same hard blow that reaches the
 * threshold also overshoots the state you wanted, while a gear train keeps the gentle blow's
 * control and spends capital and space instead.
 *
 * <h2>Two sizes, and what meshing means</h2>
 * A cog is bolted to a shaft, so it couples along its own axis exactly as a shaft does. Set two
 * cogs side by side in the same plane and their teeth <em>mesh</em> instead: they drive each
 * other, in opposite directions, at the ratio of their sizes. Small to small is 1:1 reversed;
 * large to small doubles the speed; small to large halves it.
 *
 * <h2>Where this deliberately differs from Create</h2>
 * Create requires a large cog and a small one to be placed <em>diagonally</em>, because its large
 * cog is visibly bigger than one block and its teeth reach the corner. Ours mesh face to face,
 * for as long as all the art is placeholder: every block here is a one-metre cube, so a diagonal
 * rule would be one the player cannot see and therefore cannot learn. Revisit when the cogs have
 * real models -- the propagator would need to scan diagonal neighbours, which is the only part of
 * this that is engine work rather than content.
 * <p>
 * The two sizes and their ratios are Create's, taken deliberately. Their code is MIT and the
 * rotation layer is openly Create-inspired; this mod's originality is in overrun and
 * instrumentation, not in inventing a third cog. <b>Their assets are not MIT</b> -- see
 * THIRD-PARTY-LICENSES.md.
 */
public class CogBlock extends RotatedPillarBlock implements EntityBlock, Rotatable {

    /**
     * Relative tooth count. Only the ratio between two cogs is ever used, so these are sizes
     * rather than counts -- there is no number of teeth modelled anywhere.
     */
    private final int teeth;

    private final Drag dragSuPerRpm;
    private final Inertia inertia;

    private final VoxelShape shapeX;
    private final VoxelShape shapeY;
    private final VoxelShape shapeZ;

    public CogBlock(Properties properties, int teeth, int radius, int thickness,
                    Drag dragSuPerRpm, Inertia inertia) {
        super(properties);
        this.teeth = teeth;
        this.dragSuPerRpm = dragSuPerRpm;
        this.inertia = inertia;

        float half = thickness / 2f;
        float near = 8 - half;
        float far = 8 + half;
        float low = 8 - radius;
        float high = 8 + radius;
        this.shapeX = box(near, low, low, far, high, high);
        this.shapeY = box(low, near, low, high, far, high);
        this.shapeZ = box(low, low, near, high, high, far);
    }

    public int getTeeth() {
        return teeth;
    }

    public Drag getDragSuPerRpm() {
        return dragSuPerRpm;
    }

    public Inertia getInertia() {
        return inertia;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(AXIS)) {
            case X -> shapeX;
            case Y -> shapeY;
            case Z -> shapeZ;
        };
    }

    /**
     * Inherit the axis of whatever this is placed against.
     * <p>
     * Placing by clicked face is right for a shaft, whose axis is the direction you are building,
     * and wrong for a cog, which is almost always going onto the line you just clicked or beside
     * the cog you just placed. Sneak to fall back to the clicked face.
     */
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

    /**
     * Teeth engage only with another cog lying in the same plane and set beside it. In line along
     * the axis is not meshing: that is two cogs bolted to one shaft, which the shaft coupling
     * above already handles and which turn as one.
     */
    @Override
    public float meshRatioTowards(BlockState state, Direction face, BlockState other) {
        if (!(other.getBlock() instanceof CogBlock otherCog))
            return 0;
        Direction.Axis axis = state.getValue(AXIS);
        if (axis != other.getValue(AXIS) || face.getAxis() == axis)
            return 0;
        return -(float) teeth / otherCog.teeth;
    }

    /** Drawn by the visual rather than the chunk mesh, so it can turn. See {@code ShaftBlock}. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CogBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? (l, p, s, be) -> ((CogBlockEntity) be).tickClient() : null;
    }
}
