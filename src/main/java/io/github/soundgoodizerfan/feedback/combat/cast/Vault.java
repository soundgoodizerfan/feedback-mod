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

import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Vault: spring off a target's shoulders and land behind it. */
final class Vault {

    private Vault() {
    }

    /** Vault: leap over the nearest thing in front and land behind it; it is staggered by your mass pushed off it. */
    static boolean act(ServerPlayer p) {
        LivingEntity t = ahead(p, 4, 0.5);
        if (t == null || p.isPassenger())
            return false;
        Vec3 over = t.position().subtract(p.position()).multiply(1, 0, 1);
        if (over.lengthSqr() < 1e-6)
            return false;
        over = over.normalize();
        double reach = t.distanceTo(p) + 1.8;
        p.setDeltaMovement(over.x * reach / 8.5, 0.62, over.z * reach / 8.5); // ~16 ticks aloft, air drag 0.91 a tick
        p.hurtMarked = true;
        p.fallDistance = 0f;
        stagger(p, t, (BodyMass.of(p) + BodyMass.armour(p)) * 9.8f * 1.2f);
        ServerLevel lv = p.serverLevel();
        Vec3 shoulders = t.position().add(0, t.getBbHeight(), 0);
        lv.sendParticles(ParticleTypes.CRIT, shoulders.x, shoulders.y, shoulders.z, 10, 0.3, 0.1, 0.3, 0.2);
        CastFx.ring(lv, CastFx.dust(CastFx.GOLD, 1.1f), shoulders, 0.5, 10, 0);
        CastFx.line(lv, CastFx.fade(CastFx.GOLD, 0xFFFFFF, 1.1f), p.position().add(0, 0.5, 0), shoulders, 0.25);
        CastFx.sound(lv, shoulders, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.8f, 1.2f);
        CombatCues.everyone(p, FSounds.ROLL_WHOOSH.get(), FeelTuning.ROLL_VOLUME, 1.1f);
        return true;
    }
}
