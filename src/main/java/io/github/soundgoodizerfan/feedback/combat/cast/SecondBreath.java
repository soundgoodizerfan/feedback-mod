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

package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;

/**
 * Second breath (Proficiency): a deep breath that refills stamina to sixty and pauses its regen, as a rest does.
 * Costs no stamina itself, so it is the one cast that can be slotted to answer an empty bar. Worn armour presses on the
 * chest: every {@link #MU_PER_POINT} Mu of it ({@link BodyMass#armour}) takes a point off the refill, down to
 * {@link #FLOOR}. It rearms on a landed blow, not the clock: a free refill on a timer was a cost paid by waiting.
 */
final class SecondBreath {

    /** STAND-IN: full diamond (about 50 Mu) breathes to 40, leather to about 53. */
    static final float TO = 60f, FLOOR = 30f, MU_PER_POINT = 2.5f;

    private SecondBreath() {
    }

    static boolean act(ServerPlayer p) {
        float to = Math.max(FLOOR, TO - BodyMass.armour(p) / MU_PER_POINT);
        if (Stamina.get(p) >= to)
            return false;
        Stamina.restore(p, to);
        Vec3 at = p.position().add(0, 1.6, 0);
        p.serverLevel().sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 8, 0.3, 0.3, 0.3, 0.02);
        CastFx.sound(p.serverLevel(), at, SoundEvents.ARMOR_EQUIP_CHAIN.value(), 0.8f, 0.8f);
        return true;
    }
}
