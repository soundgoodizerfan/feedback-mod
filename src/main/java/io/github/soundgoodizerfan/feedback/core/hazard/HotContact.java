// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.hazard;

import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;

import net.minecraft.world.entity.LivingEntity;

/**
 * What a hot thing does to the skin that meets it: three named bands, in damage and in fire.
 *
 * <h2>Why bands, and why these</h2>
 * Philosophy 3: a free scalar has a right answer in it and a named set does not. A scald, a burn
 * and a sear are three things a player can recognise and plan around -- the same trade the
 * temperature adjectives make for the eye -- and the thresholds sit on figures the mod already
 * has (water boils, wood chars, a piece starts to glow), so nothing here is a new number to learn.
 * The damage is in {@link Hazards#HOT_CONTACT}, fire-tagged, so fire resistance is a real answer.
 */
public final class HotContact {

    public enum Band {
        NONE(0f, 0),
        SCALD(HazardTuning.CONTACT_PAIN_DAMAGE, 0),
        BURN(HazardTuning.CONTACT_BURN_DAMAGE, HazardTuning.CONTACT_BURN_IGNITE_SECONDS),
        SEAR(HazardTuning.CONTACT_SEAR_DAMAGE, HazardTuning.CONTACT_SEAR_IGNITE_SECONDS);

        public final float damage;
        public final int igniteSeconds;

        Band(float damage, int igniteSeconds) {
            this.damage = damage;
            this.igniteSeconds = igniteSeconds;
        }
    }

    private HotContact() {
    }

    public static Band band(Tu tu) {
        float t = tu.value();
        if (t >= HazardTuning.CONTACT_SEAR_TU.value())
            return Band.SEAR;
        if (t >= HazardTuning.CONTACT_BURN_TU.value())
            return Band.BURN;
        if (t >= ThermalTuning.CONTACT_PAIN_TU.value())
            return Band.SCALD;
        return Band.NONE;
    }

    /**
     * Burn {@code victim} as a piece at {@code tu} would. Returns whether anything was done.
     * Invulnerability frames are left alone: a hot hand is a recurring cost, not a burst.
     */
    public static boolean burn(LivingEntity victim, Tu tu) {
        Band band = band(tu);
        if (band == Band.NONE || victim.level().isClientSide)
            return false;
        boolean hurt = victim.hurt(Hazards.source(victim.level(), Hazards.HOT_CONTACT), band.damage);
        if (band.igniteSeconds > 0)
            victim.igniteForSeconds(band.igniteSeconds);
        if (hurt && victim instanceof net.minecraft.world.entity.player.Player player)
            io.github.soundgoodizerfan.feedback.core.Awards.give(player, "progress/joke/hot_take");
        return hurt;
    }
}
