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
        SCALD(FTuning.CONTACT_PAIN_DAMAGE, 0),
        BURN(FTuning.CONTACT_BURN_DAMAGE, FTuning.CONTACT_BURN_IGNITE_SECONDS),
        SEAR(FTuning.CONTACT_SEAR_DAMAGE, FTuning.CONTACT_SEAR_IGNITE_SECONDS);

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
        if (t >= FTuning.CONTACT_SEAR_TU.value())
            return Band.SEAR;
        if (t >= FTuning.CONTACT_BURN_TU.value())
            return Band.BURN;
        if (t >= FTuning.CONTACT_PAIN_TU.value())
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
        return hurt;
    }
}
