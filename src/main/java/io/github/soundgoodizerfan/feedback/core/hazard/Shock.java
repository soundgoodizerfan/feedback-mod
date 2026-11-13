// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.hazard;

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
 * can read off a source and it is the vocabulary electrical hazards share with
 * everything else. The bottom band keeps the figure the bare wire always had, so this widens that
 * rule instead of adding a second one.
 *
 * <h2>Resistance is still not modelled</h2>
 * The wire's current does not enter. A shock is a property of a conductor being <em>exposed</em>
 * at a voltage, which is what a player can see and fix; how much current would flow through a
 * particular hand is exactly the variable philosophy 21 declines. What does scale a shock is
 * {@link Exposure}: a multiplier read off the victim's surroundings (wet, armour, footing), not
 * a model of its body.
 */
public final class Shock {

    private Shock() {
    }

    public static float damage(float volts) {
        return damage(ElectricRules.bandOfPotential(volts));
    }

    public static float damage(VoltageBand band) {
        return switch (band) {
            case LV -> HazardTuning.BARE_WIRE_SHOCK_DAMAGE;
            case MV -> HazardTuning.SHOCK_MV_DAMAGE;
            case HV -> HazardTuning.SHOCK_HV_DAMAGE;
            case UHV -> HazardTuning.SHOCK_UHV_DAMAGE;
            case EHV -> HazardTuning.SHOCK_EHV_DAMAGE;
            case TRUNK -> HazardTuning.SHOCK_TRUNK_DAMAGE;
        };
    }

    /** Shock {@code victim} as a live conductor of {@code band} would; null is no shock. Returns whether it hurt. */
    public static boolean apply(Entity victim, @org.jetbrains.annotations.Nullable VoltageBand band) {
        return band != null && apply(victim, ElectricRules.bandPotential(band));
    }

    /** Shock {@code victim} as a discharge of {@code volts} (an electrochemical or static potential, never shown) would. Returns whether it hurt. */
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
