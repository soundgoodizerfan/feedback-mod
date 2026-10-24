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

import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Stomp: the whole of your weight on something already knocked off its feet. */
final class Stomp {

    private Stomp() {
    }

    /** Stomp: on the nearest staggered thing within reach, your mass comes down as damage and a fresh stagger. Nothing that is standing steady will take it. */
    static boolean act(ServerPlayer p) {
        LivingEntity t = null;
        double best = 9;
        for (LivingEntity e : near(p, p.position(), 3)) {
            double d = e.distanceToSqr(p);
            if (CombatState.of(e).phase() == CombatPhase.STAGGER && d < best) {
                best = d;
                t = e;
            }
        }
        if (t == null)
            return false;
        float mass = BodyMass.of(p) + BodyMass.armour(p);
        float damage = (3f + mass / 10f) * (Skills.unlocked(p, "crush") ? 1.5f : 1f);
        t.invulnerableTime = 0;
        t.hurt(p.damageSources().playerAttack(p), damage);
        stagger(p, t, mass * 9.8f * 1.5f);
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        ServerLevel lv = p.serverLevel();
        Vec3 at = t.position().add(0, 0.2, 0);
        lv.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        CastFx.shockwave(p, at, 2.0, 3, ParticleTypes.CLOUD, 0.25);
        CastFx.debris(lv, at, 25, 0.8);
        CastFx.sound(lv, at, SoundEvents.ANVIL_LAND, 1f, 0.5f);
        CastFx.sound(lv, at, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 0.6f);
        return true;
    }
}
