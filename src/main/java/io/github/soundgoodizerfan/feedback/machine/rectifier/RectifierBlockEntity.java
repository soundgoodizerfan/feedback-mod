// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.rectifier;

import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A fast-charge fitting (roadmap E4): a SINK at MV that asks a small standing {@link ElectricMachineTuning#RECTIFIER_AMPS}. While it is
 * fed ({@link #isPowered}), a battery touching it charges in {@link ElectricMachineTuning#RECTIFIER_CHARGE_TICKS} instead of its usual
 * time; the battery reads this, the rectifier changes nothing itself. Alone it does nothing but draw its standing amps.
 * It has no wear and no thermal body (shortcut: the standing draw is its whole loss; give it a body if a rectifier ever
 * has to overheat).
 */
public class RectifierBlockEntity extends ElectricNode {

    public RectifierBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.RECTIFIER.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return ElectricMachineTuning.RECTIFIER_AMPS;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.MV;
    }

    /** Whether the line delivers it current right now. */
    public boolean isPowered() {
        return getAmps() > 0f && !isBlocked();
    }
}
