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
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.later;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Flurry: six fast blows with empty hands. */
final class Flurry {

    private Flurry() {
    }

    /** Six fast blows with empty hands on whoever is in front, the last one a stagger. */
    static boolean act(ServerPlayer p) {
        if (!p.getMainHandItem().isEmpty())
            return false;
        for (int i = 0; i < 6; i++) {
            boolean last = i == 5;
            int n = i;
            later(p, i * 2, () -> {
                if (!p.isAlive() || !p.getMainHandItem().isEmpty())
                    return;
                Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
                LivingEntity best = null;
                double bestDot = 0.6;
                for (LivingEntity e : near(p, eye.add(look.scale(1.8)), 2.2)) {
                    Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
                    double d = to.normalize().dot(look);
                    if (to.length() <= 3.5 && d > bestDot) {
                        best = e;
                        bestDot = d;
                    }
                }
                p.swing((p.getRandom().nextBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND), true);
                if (best == null)
                    return;
                best.invulnerableTime = 0;
                best.hurt(p.damageSources().playerAttack(p), 2f);
                ServerLevel lv = p.serverLevel();
                Vec3 hit = best.getBoundingBox().getCenter().add((lv.random.nextDouble() - 0.5) * 0.5, (lv.random.nextDouble() - 0.5) * 0.4, (lv.random.nextDouble() - 0.5) * 0.5);
                // each fist leaves a streak and a spark; the pitch climbs a blow at a time
                CastFx.line(lv, CastFx.fade(CastFx.GOLD, 0xFFFFFF, 1.0f), eye.add(look.scale(0.5)).add(0, -0.3, 0), hit, 0.2);
                lv.sendParticles(ParticleTypes.CRIT, hit.x, hit.y, hit.z, 6, 0.15, 0.15, 0.15, 0.3);
                lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, hit.x, hit.y, hit.z, 3, 0.1, 0.1, 0.1, 0.3);
                CastFx.sound(lv, hit, SoundEvents.PLAYER_ATTACK_STRONG, 0.8f, 1.0f + n * 0.12f);
                if (last) {
                    stagger(p, best, 6f * ParryTuning.JOULES_PER_DAMAGE);
                    lv.sendParticles(ParticleTypes.EXPLOSION, hit.x, hit.y, hit.z, 1, 0, 0, 0, 0);
                    lv.sendParticles(ParticleTypes.FLASH, hit.x, hit.y, hit.z, 1, 0, 0, 0, 0);
                    CastFx.shockwave(p, best.position().add(0, 0.1, 0), 2.5, 4, CastFx.dust(CastFx.GOLD, 1.3f), 0);
                    CastFx.sound(lv, hit, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.8f);
                    CastFx.sound(lv, hit, SoundEvents.ANVIL_LAND, 0.6f, 0.8f);
                }
                CombatCues.everyone(p, FSounds.FEEL_LIGHT.get(), FeelTuning.LIGHT_VOLUME, 1.1f);
            });
        }
        return true;
    }
}
