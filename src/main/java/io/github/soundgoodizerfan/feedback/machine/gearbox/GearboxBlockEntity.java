// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.gearbox;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A gearbox on the network: a relay that drags more than a shaft, because it is four meshes
 * rather than a pair of bearings. Nothing else -- the direction change is entirely a property of
 * the block, and the propagator works it out from the face.
 */
public class GearboxBlockEntity extends RotationNode {

    public GearboxBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.GEARBOX.get(), pos, state);
    }

    @Override
    public Drag getDragSuPerRpm() {
        return FTuning.GEARBOX_DRAG_SU_PER_RPM;
    }

    @Override
    public Inertia getInertia() {
        return FTuning.GEARBOX_INERTIA;
    }
}
