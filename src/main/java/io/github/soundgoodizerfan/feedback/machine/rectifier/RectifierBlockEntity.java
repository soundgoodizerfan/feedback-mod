/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.machine.rectifier;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A fast-charge fitting (roadmap E4): a SINK at MV that asks a small standing {@link FTuning#RECTIFIER_AMPS}. While it is
 * fed ({@link #isPowered}), a battery touching it charges in {@link FTuning#RECTIFIER_CHARGE_TICKS} instead of its usual
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
        return FTuning.RECTIFIER_AMPS;
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
