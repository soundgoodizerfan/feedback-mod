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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.process.Material;

/**
 * What a packed charge does when it goes up, from its blend and its casing: one rule for the placed
 * block ({@link DemolitionChargeBlockEntity}) and for the figures a charge is designed against
 * (the Blueprint Table, {@code ModularBake}), so the plan and the blast cannot drift.
 */
public final class Blast {

    private Blast() {
    }

    /** How much the casing holds the gas in until it lets go: 1 bare, more for a stronger metal, to a cap. */
    public static float confinement(Material casing) {
        if (casing == null)
            return 1f;
        return 1f + Math.min(FTuning.DEMO_CONFINE_MAX, casing.mechanical().yieldStrength().map(y -> y.value()).orElse(0f)
                * FTuning.DEMO_CONFINE_PER_ST);
    }

    /** Explosion power: the square root of the burned potential, times the casing's confinement, capped. */
    public static float power(Cartridge.Blend blend, Material casing) {
        if (blend.coil())
            return 0f;
        return Math.min(FTuning.DEMO_POWER_MAX,
                FTuning.DEMO_POWER_PER_SQRT * (float) Math.sqrt(blend.potential() * blend.burned()) * confinement(casing));
    }

    /** Whether what did not burn goes up as fire: a fuel-rich charge. */
    public static boolean fire(Cartridge.Blend blend) {
        return blend.burned() < FTuning.DEMO_FIRE_BELOW_BURNED;
    }
}
