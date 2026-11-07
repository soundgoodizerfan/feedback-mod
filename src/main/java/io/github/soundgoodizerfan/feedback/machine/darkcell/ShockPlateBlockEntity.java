// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.darkcell;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.registry.FAutomation;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/** The shock plate's sink: it asks for current, and the harm it does is the power the run delivers. */
public class ShockPlateBlockEntity extends ElectricNode implements KillMethod {

    /** Invented. Amps it asks for. */
    private static final float AMPS = 4f;
    /** Invented. Watts per point of harm. */
    private static final float WATTS_PER_DAMAGE = 25f;

    private float watts;

    public ShockPlateBlockEntity(BlockPos pos, BlockState state) {
        super(FAutomation.SHOCK_PLATE_ENTITY.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.MV;
    }

    @Override
    public float wantedAmps() {
        return AMPS;
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        watts = volts * amps;
    }

    @Override
    public KillKind kind() {
        return KillKind.SHOCK;
    }

    @Override
    public boolean ready() {
        return watts > 0f;
    }

    @Override
    public float damage(MobFacts facts) {
        return facts.electricImmune() ? 0f : watts / WATTS_PER_DAMAGE;
    }
}
