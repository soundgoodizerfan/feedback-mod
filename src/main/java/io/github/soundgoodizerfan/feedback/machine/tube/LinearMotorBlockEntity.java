// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.tube;

import io.github.soundgoodizerfan.feedback.core.tube.TubeTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An electric sink asking {@link TubeTuning#LINEAR_MOTOR_AMPS} at MV. {@link #powered} is true while the
 * run feeds it at least half of what it wants; tube blocks beside it read that when they time a hop.
 * Only a capsule with a magnetic plate answers to it (the same property again, spec 6.1).
 */
public class LinearMotorBlockEntity extends ElectricNode {

    private boolean powered;

    public LinearMotorBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.LINEAR_MOTOR.get(), pos, state);
    }

    public boolean powered() {
        return powered;
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return TubeTuning.LINEAR_MOTOR_AMPS;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.MV;
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        powered = ElectricRules.rate(volts, amps, wanted, ratedBand()) >= 0.5f;
    }
}
