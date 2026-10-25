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

import io.github.soundgoodizerfan.feedback.combat.hit.BareMode;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.later;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Haymaker: a slow wound-up punch that sends a body flying, and leaves you open if it finds nothing. */
final class Haymaker {

    private Haymaker() {
    }

    /** Haymaker: wind up for half a second, then throw one punch for nine damage that shoves a body back by your mass over its. A miss leaves you staggered. */
    static boolean act(ServerPlayer p) {
        if (!BareMode.mainHand(p).isEmpty())
            return false;
        Vec3 hand = p.getEyePosition().add(p.getLookAngle().scale(0.6)).add(0, -0.4, 0);
        CastFx.converge(p, hand, 1.4, 10, CastFx.dust(CastFx.EMBER, 1.2f));
        CastFx.sound(p.serverLevel(), hand, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.6f, 0.5f);
        CastFx.clip(p, "one_handed_swipe_horizontal_right", CastFx.MAIN, 20, 10);
        later(p, 10, () -> {
            if (!p.isAlive())
                return;
            p.swing(InteractionHand.MAIN_HAND, true);
            LivingEntity t = ahead(p, 3.4, 0.45);
            ServerLevel lv = p.serverLevel();
            if (t == null) {
                CombatTicker.stagger(p, 25, ParryTuning.STAGGER_SLOW); // the weight of a whiff
                lv.sendParticles(ParticleTypes.CLOUD, hand.x, hand.y, hand.z, 6, 0.3, 0.2, 0.3, 0.05);
                return;
            }
            float mine = BodyMass.of(p) + BodyMass.armour(p), theirs = Math.max(1f, BodyMass.of(t) + BodyMass.armour(t));
            Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
            double shove = Mth.clamp(mine / theirs, 0.2, 2.0) * 0.9;
            t.invulnerableTime = 0;
            t.hurt(p.damageSources().playerAttack(p), 9f);
            t.push(look.x * shove, 0.3, look.z * shove);
            t.hurtMarked = true;
            stagger(p, t, 9f * ParryTuning.JOULES_PER_DAMAGE);
            Vec3 c = t.getBoundingBox().getCenter();
            lv.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 1, 0, 0, 0, 0);
            lv.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 24, 0.3, 0.3, 0.3, 0.6);
            CastFx.shockwave(p, t.position().add(0, 0.1, 0), 2.4, 4, CastFx.dust(CastFx.EMBER, 1.3f), 0);
            CastFx.sound(lv, c, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.5f);
            CastFx.sound(lv, c, SoundEvents.ANVIL_LAND, 0.6f, 1.3f);
        });
        return true;
    }
}
