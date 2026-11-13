// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.cog;

import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationNode;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A cog on the network. Carries no gearing logic at all -- the ratio is a property of how two
 * cogs are placed, which is {@link CogBlock}'s business and the propagator's.
 * <p>
 * One block entity type serves both sizes, and the figures are read off the block rather than
 * held here. Anything stored on the block entity would have to be written, read back and kept in
 * step with the block it belongs to, to describe something that cannot change without the block
 * being replaced.
 */
public class CogBlockEntity extends RotationNode {

    public CogBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.COG.get(), pos, state);
    }

    @Override
    public Drag getDragSuPerRpm() {
        return getBlockState().getBlock() instanceof CogBlock cog
                ? cog.getDragSuPerRpm()
                : MachineTuning.SMALL_COG_DRAG_SU_PER_RPM;
    }

    @Override
    public Inertia getInertia() {
        return getBlockState().getBlock() instanceof CogBlock cog
                ? cog.getInertia()
                : MachineTuning.SMALL_COG_INERTIA;
    }
}
