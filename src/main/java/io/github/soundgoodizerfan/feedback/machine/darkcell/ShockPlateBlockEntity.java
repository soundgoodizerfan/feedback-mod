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
