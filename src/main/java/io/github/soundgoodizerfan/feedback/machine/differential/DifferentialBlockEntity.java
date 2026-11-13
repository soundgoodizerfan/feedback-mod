// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.differential;

import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** A differential on the network: the input's run member, which feeds the two axles once a tick. */
public class DifferentialBlockEntity extends RotationNode {

    /** How far the shares have moved from even, -1 to 1; smoothed over ticks. */
    private float share;

    public DifferentialBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.DIFFERENTIAL.get(), pos, state);
    }

    @Override
    public Drag getDragSuPerRpm() {
        return MachineTuning.GEARBOX_DRAG_SU_PER_RPM;
    }

    @Override
    public Inertia getInertia() {
        return MachineTuning.GEARBOX_INERTIA;
    }

    public void tickServer() {
        if (level != null && !level.isClientSide)
            share = DifferentialBlock.step(level, this, getBlockState(), share);
    }
}
