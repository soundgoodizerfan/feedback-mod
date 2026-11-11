// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.cog;

import io.github.soundgoodizerfan.feedback.core.rotation.MicroRotation;
import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
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
import net.minecraft.world.phys.shapes.Shapes;
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
 * cogs where their teeth reach and they <em>mesh</em> instead: they drive each other at the ratio
 * of their sizes. Which positions reach is Create's rule (user decision 2026-11-02, "make them like
 * Create"): small to small face to face in one plane; small to large diagonally in one plane, the
 * small against the large one's rim; and two large cogs at right angles, diagonally, as a bevel
 * pair. The large cog is drawn wider than its block (its model reaches into the neighbours) so the
 * rule can be seen; it still occupies one block. Positions and signs: {@code
 * docs/specs/feedback_micro_spec.md}, <em>Cog meshing</em>. This replaced a face-to-face-only rule that
 * suited placeholder cubes and stopped once the large cog was drawn at its size.
 * <p>
 * The two sizes and their ratios are Create's, taken deliberately. Their code is MIT and the
 * rotation layer is openly Create-inspired; this mod's originality is in overrun and
 * instrumentation, not in inventing a third cog. <b>Their assets are not MIT</b> -- see
 * THIRD-PARTY-LICENSES.md.
 */
public class CogBlock extends RotatedPillarBlock implements EntityBlock, MicroRotation {

    /**
     * Relative tooth count. Only the ratio between two cogs is ever used, so these are sizes
     * rather than counts -- there is no number of teeth modelled anywhere.
     */
    private final int teeth;

    /** The two sizes' teeth: a large cog is twice a small one, Create's proportion. */
    public static final int SMALL = 1, LARGE = 2;

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
        // The disc (clamped to the block by the radius given) and the hub through the block: a large
        // cog's drawn disc reaches into its neighbours, its shape stays in its own block.
        this.shapeX = Shapes.or(box(near, low, low, far, high, high), box(0, 6, 6, 16, 10, 10));
        this.shapeY = Shapes.or(box(low, near, low, high, far, high), box(6, 0, 6, 10, 16, 10));
        this.shapeZ = Shapes.or(box(low, low, near, high, high, far), box(6, 6, 0, 10, 10, 16));
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
        return getStateForPlacement(context, context.getLevel()
                .getBlockState(context.getClickedPos().relative(context.getClickedFace().getOpposite())));
    }

    /** The rule above, with {@code against} given: the world's block full size, the clicked voxel as a micro-block. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context, @Nullable BlockState against) {
        Direction face = context.getClickedFace();
        if ((context.getPlayer() == null || !context.getPlayer().isShiftKeyDown())
                && against != null && against.getBlock() instanceof Rotatable rotatable)
            return defaultBlockState().setValue(AXIS, rotatable.getRotationAxis(against));
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
     * Face to face, only two small cogs in one plane mesh (1:1, reversed): a large cog's rim reaches
     * past the next block, so face to face it would pass through its neighbour instead. In line along
     * the axis is not meshing: that is two cogs bolted to one shaft, which the shaft coupling above
     * already handles and which turn as one. Create's rule, see the micro spec's <em>Cog meshing</em>.
     */
    @Override
    public float meshRatioTowards(BlockState state, Direction face, BlockState other) {
        return faceMesh(teeth, state.getValue(AXIS), face, other);
    }

    @Override
    public int teeth(BlockState state) {
        return teeth;
    }

    /** The teeth of {@code other}, 0 if it has none. */
    private static int teethOf(BlockState other) {
        return other.getBlock() instanceof Rotatable r ? r.teeth(other) : 0;
    }

    /**
     * The face rule for any toothed block (a cog, a ring gear) with {@code teeth} on {@code axis}:
     * only two small ones in one plane, 1:1 reversed.
     */
    public static float faceMesh(int teeth, Direction.Axis axis, Direction face, BlockState other) {
        if (teeth != SMALL || teethOf(other) != SMALL)
            return 0;
        if (axis != ((Rotatable) other.getBlock()).getRotationAxis(other) || face.getAxis() == axis)
            return 0;
        return -1;
    }

    @Override
    public boolean meshesDiagonally(BlockState state) {
        return true;
    }

    /**
     * Diagonally, two pairings mesh (Create's, see the micro spec's <em>Cog meshing</em>): a small and
     * a large cog on one axis, diagonal in their plane, the small against the large one's rim, at the
     * teeth ratio reversed; and two large cogs at right angles, one step along each one's axis and
     * none along the third -- a bevel pair, whose contact point at the shared corner makes the
     * neighbour's speed this one's times (offset along the neighbour's axis) over (offset along this
     * one's axis).
     */
    @Override
    public float meshRatioAt(BlockState state, Vec3i offset, BlockState other) {
        return diagonalMesh(teeth, state.getValue(AXIS), offset, other);
    }

    /** The diagonal rules for any toothed block with {@code teeth} on {@code axis}; see above. */
    public static float diagonalMesh(int teeth, Direction.Axis axis, Vec3i offset, BlockState other) {
        int otherTeeth = teethOf(other);
        if (otherTeeth == 0)
            return 0;
        Direction.Axis otherAxis = ((Rotatable) other.getBlock()).getRotationAxis(other);
        int along = axis.choose(offset.getX(), offset.getY(), offset.getZ());
        if (axis == otherAxis)
            return along == 0 && teeth != otherTeeth ? -(float) teeth / otherTeeth : 0;
        int alongOther = otherAxis.choose(offset.getX(), offset.getY(), offset.getZ());
        if (teeth != LARGE || otherTeeth != LARGE || along == 0 || alongOther == 0)
            return 0;
        return (float) alongOther / along;
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
