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
 * Damage follows the shock's {@link VoltageBand} and its Eu: the band sets the figure of one packet (the bottom band keeps the
 * figure the bare wire always had) and the Eu in the discharge, in packets, scales it between a floor and a ceiling. A glove is
 * rated to a band and stops a shock of that band or below.
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

    /** Base damage of a shock of {@code band}: the figure of one whole packet. */
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

    /** Damage of a discharge of {@code eu} at {@code band}: the band's figure times the packets' worth of Eu in it, held between the tuning's floor and ceiling. */
    public static float damage(VoltageBand band, float eu) {
        float packets = eu / ElectricRules.packetEu(band);
        return damage(band) * Math.max(HazardTuning.SHOCK_MIN_PACKETS, Math.min(HazardTuning.SHOCK_MAX_PACKETS, packets));
    }

    /** Shock {@code victim} as a bare, live conductor of {@code band} would (one packet); null is no shock. Returns whether it hurt. */
    public static boolean apply(Entity victim, @org.jetbrains.annotations.Nullable VoltageBand band) {
        return band != null && apply(victim, band, ElectricRules.packetEu(band));
    }

    /** Shock {@code victim} as a discharge of {@code eu} at {@code band} (a jar, a creeper's charge, an arc) would. A glove rated to the band stops it. Returns whether it hurt. */
    public static boolean apply(Entity victim, VoltageBand band, float eu) {
        if (victim.level().isClientSide || eu <= 0f)
            return false;
        if (victim instanceof Player player && Protection.shockProof(player, band))
            return false;
        // Exposure scales the figure for any victim; a dry bare thing on stone reads 1.0.
        return victim.hurt(Hazards.source(victim.level(), Hazards.SHOCK), damage(band, eu) * Exposure.of(victim)
                * (victim instanceof LivingEntity body ? Ward.passes(body, Ward.Hazard.SHOCK) : 1f));
    }
}
