// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.linkage;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.core.unit.Su;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Counts out strokes from rotation and hands them to whatever it is pointed at.
 *
 * <h2>Why the load is borrowed rather than owned</h2>
 * The linkage reports the driven machine's Su as its own, so a hammer's 80 Su appears on the
 * rotation network even though the hammer is not on it. Otherwise a machine could be driven for
 * free simply because a converter sat between it and the shaft.
 */
public class CrankLinkageBlockEntity extends RotationNode {

    /** Fractional strokes accumulated since the last one landed. */
    private float strokeProgress;

    public CrankLinkageBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.CRANK_LINKAGE.get(), pos, state);
    }

    public Throw getThrow() {
        return getBlockState().getValue(CrankLinkageBlock.THROW);
    }

    private Direction getFacing() {
        return getBlockState().getValue(CrankLinkageBlock.FACING);
    }

    /** The reciprocating machine in front, if there is one. */
    @Nullable
    public Reciprocating getDriven() {
        if (level == null)
            return null;
        BlockEntity be = level.getBlockEntity(worldPosition.relative(getFacing()));
        return be instanceof Reciprocating reciprocating ? reciprocating : null;
    }

    public void tickServer() {
        float rpm = Math.abs(getRpm().value());
        if (rpm <= 0) {
            strokeProgress = 0;
            return;
        }

        strokeProgress += rpm * FTuning.STROKES_PER_RPM_PER_TICK;
        if (strokeProgress < 1)
            return;

        Reciprocating driven = getDriven();
        // Count down rather than zeroing, so a fast linkage can land more than one stroke per
        // tick instead of silently throwing the extras away.
        while (strokeProgress >= 1) {
            strokeProgress -= 1;
            if (driven != null)
                driven.onStroke(getThrow(), getGearAdvantage());
        }
    }

    /**
     * Whether a linkage is turning in front of the machine at this position.
     *
     * <h2>Why an instrument asks the drive and not the machine</h2>
     * A reciprocating machine has no idea whether it is working. It is told, one stroke at a time,
     * and between strokes it looks exactly like a stopped one -- so "is this hammer running" is a
     * question about the shaft, the same way overstress is a question about the network rather
     * than about any node. Asking the six neighbours is the whole of it: a linkage drives what it
     * faces, and nothing else reaches a machine.
     */
    public static boolean isDriving(@Nullable Level level, BlockPos machinePos) {
        return driving(level, machinePos) != null;
    }

    /** The linkage turning and facing the machine at {@code machinePos}, or null: what {@link #isDriving} asks. */
    @Nullable
    public static CrankLinkageBlockEntity driving(@Nullable Level level, BlockPos machinePos) {
        if (level == null)
            return null;
        for (Direction from : Direction.values()) {
            if (level.getBlockEntity(machinePos.relative(from)) instanceof CrankLinkageBlockEntity linkage
                    && linkage.getFacing() == from.getOpposite()
                    && Math.abs(linkage.getRpm().value()) > FTuning.STOPPED_RPM_THRESHOLD.value())
                return linkage;
        }
        return null;
    }

    /**
     * How much the gearing between the source and this linkage multiplies the force behind a blow.
     *
     * <h2>Why this is the reciprocal of the speed ratio</h2>
     * A gear train trades speed for force and keeps the product: turn at half the speed and you
     * turn with twice the torque. The linkage already runs at its own geared speed, so blows fall
     * half as often on their own -- work per second is unchanged, and what changes is whether any
     * one blow clears the material's hardness floor at all. That is philosophy §6's second answer
     * to the force problem: the short throw spends precision, a gear train spends capital and
     * space.
     *
     * <h2>The caveat worth knowing</h2>
     * Ratios are measured against whichever node the run is anchored to, which is the driving
     * source whenever there is one. A run with no source at all is coasting, and its anchor is
     * arbitrary -- so the force behind a blow struck purely on momentum is scaled against nothing
     * in particular. Left alone: a coasting run is stopping, and nothing is asking it for work.
     */
    public float getGearAdvantage() {
        float ratio = Math.abs(getRatio());
        return ratio == 0 ? 1 : 1 / ratio;
    }

    @Override
    public Su getLoadSu() {
        Reciprocating driven = getDriven();
        return driven == null ? NO_SU : driven.getLoadSu();
    }

    @Override
    public Drag getDragSuPerRpm() {
        return FTuning.SHAFT_DRAG_SU_PER_RPM;
    }

    @Override
    public Inertia getInertia() {
        return FTuning.SHAFT_INERTIA;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("StrokeProgress", strokeProgress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        strokeProgress = tag.getFloat("StrokeProgress");
    }
}
