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
import io.github.soundgoodizerfan.feedback.combat.hit.FistTuning;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.later;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Uppercut: a short crouch, then a fist driven up through the chin that throws a body high into the air. */
final class Uppercut {

    /** Ticks of the whole clip and of the windup before the fist lands; Better Combat's uppercut is drawn on the off-hand slot for a fist. */
    private static final float CLIP = 16f;
    private static final int WINDUP = 6;

    private Uppercut() {
    }

    /** Uppercut: after a quick windup, one punch for seven damage that launches a body about seven blocks up. A miss leaves you staggered for a moment. */
    static boolean act(ServerPlayer p) {
        if (!BareMode.mainHand(p).isEmpty())
            return false;
        CastFx.clip(p, "one_handed_uppercut_right", CastFx.OFF, CLIP, WINDUP);
        later(p, WINDUP, () -> {
            if (!p.isAlive())
                return;
            ServerLevel lv = p.serverLevel();
            Vec3 hand = p.getEyePosition().add(p.getLookAngle().scale(0.8)).add(0, -0.3, 0);
            LivingEntity t = ahead(p, 3.2, 0.5);
            if (t == null) {
                CombatTicker.stagger(p, 15, ParryTuning.STAGGER_SLOW);
                lv.sendParticles(ParticleTypes.CLOUD, hand.x, hand.y, hand.z, 5, 0.2, 0.3, 0.2, 0.05);
                return;
            }
            t.invulnerableTime = 0;
            t.hurt(p.damageSources().playerAttack(p), 7f);
            t.setDeltaMovement(t.getDeltaMovement().x, FistTuning.LAUNCH, t.getDeltaMovement().z);
            t.hurtMarked = true;
            stagger(p, t, 7f * ParryTuning.JOULES_PER_DAMAGE);
            Vec3 c = t.getBoundingBox().getCenter();
            lv.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 20, 0.2, 0.4, 0.2, 0.5);
            CastFx.sound(lv, c, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 0.8f);
            CastFx.sound(lv, c, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.7f);
        });
        return true;
    }
}
