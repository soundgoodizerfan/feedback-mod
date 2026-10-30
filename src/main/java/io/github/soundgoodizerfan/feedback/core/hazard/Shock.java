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
package io.github.soundgoodizerfan.feedback.core.hazard;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;

import io.github.soundgoodizerfan.feedback.potion.Ward;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * What a bare, live conductor does to whatever touches it.
 *
 * <h2>Bands again</h2>
 * Damage follows the voltage's {@link VoltageBand}, never the volts: a band is a rating a player
 * can read off a source (mechanics 6.3) and it is the vocabulary electrical hazards share with
 * everything else. The bottom band keeps the figure the bare wire always had, so this widens that
 * rule instead of adding a second one.
 *
 * <h2>Resistance is still not modelled</h2>
 * The wire's current does not enter. A shock is a property of a conductor being <em>exposed</em>
 * at a voltage, which is what a player can see and fix; how much current would flow through a
 * particular hand is exactly the variable philosophy 17 declines. What does scale a shock is
 * {@link Exposure}: a multiplier read off the victim's surroundings (wet, armour, footing), not
 * a model of its body.
 */
public final class Shock {

    private Shock() {
    }

    public static float damage(float volts) {
        VoltageBand band = ElectricRules.bandOf(volts);
        return switch (band) {
            case LV -> FTuning.BARE_WIRE_SHOCK_DAMAGE;
            case MV -> FTuning.SHOCK_MV_DAMAGE;
            case HV -> FTuning.SHOCK_HV_DAMAGE;
            case UHV -> FTuning.SHOCK_UHV_DAMAGE;
        };
    }

    /** Shock {@code victim} as {@code volts} would. Returns whether it hurt. */
    public static boolean apply(Entity victim, float volts) {
        if (victim.level().isClientSide || volts <= 0f)
            return false;
        if (victim instanceof Player player && Protection.shockProofVolts(player) >= volts)
            return false;
        // Exposure scales the figure for any victim; a dry bare thing on stone reads 1.0, the old damage.
        return victim.hurt(Hazards.source(victim.level(), Hazards.SHOCK), damage(volts) * Exposure.of(victim)
                * (victim instanceof LivingEntity body ? Ward.passes(body, Ward.Hazard.SHOCK) : 1f));
    }
}
