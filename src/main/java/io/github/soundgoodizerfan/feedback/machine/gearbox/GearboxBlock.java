// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.gearbox;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.MicroRotation;
import io.github.soundgoodizerfan.feedback.core.rotation.Rotatable;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Turns rotation through a corner. Four bevels on a common crown, in a box.
 *
 * <h2>Why this is a separate block from a cog</h2>
 * Cogs change speed; the gearbox changes <em>axis</em>, at 1:1. Keeping them apart means neither
 * block does two jobs, and it is the honest split: a gear train is how you buy force, and a
 * gearbox is how you get power round a wall. Wanting both means placing both.
 *
 * <h2>Direction, and why it is not free to choose</h2>
 * Every shaft the box couples runs off the same crown, so which way each one turns is fixed by
 * geometry rather than by preference: shafts on opposite faces turn against each other, the way
 * the two halves of a differential do, and perpendicular shafts agree or disagree depending on
 * which corner they sit at. There is no consistent rule where every turn reverses -- the three
 * axis pairs cannot all be negative at once -- so the signs are taken from the face's own axis
 * direction, which is consistent by construction and reverses exactly the pairs that are opposite.
 * <p>
 * Getting direction right is therefore a placement problem, which is the intent. The gearbox has
 * no setting on it, per philosophy §3, and the player who needs the other direction adds a cog.
 */
public class GearboxBlock extends Block implements EntityBlock, MicroRotation {

    public GearboxBlock(Properties properties) {
        super(properties);
    }

    @Override
    public Drag getDragSuPerRpm() {
        return FTuning.GEARBOX_DRAG_SU_PER_RPM;
    }

    @Override
    public Inertia getInertia() {
        return FTuning.GEARBOX_INERTIA;
    }

    /**
     * A gearbox has no single axis -- that is the whole point of it. Answered only because
     * {@link Rotatable} asks; nothing reads it, because the box does not visibly turn.
     */
    @Override
    public Direction.Axis getRotationAxis(BlockState state) {
        return Direction.Axis.Y;
    }

    @Override
    public boolean hasShaftTowards(LevelAccessor level, BlockPos pos, BlockState state, Direction face) {
        return true;
    }

    @Override
    public float shaftSignTowards(BlockState state, Direction face) {
        return face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
    }

    /** Drawn by {@code client/SpinningParts}: the housing stands, the shafts turn. */
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? (l, p, s, be) -> ((io.github.soundgoodizerfan.feedback.core.rotation.RotationNode) be).tickClient() : null;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GearboxBlockEntity(pos, state);
    }
}
