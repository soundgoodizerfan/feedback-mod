// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.combat.grab.Grabbing;
import io.github.soundgoodizerfan.feedback.combat.hit.BareMode;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
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
        if (!BareMode.mainHand(p).isEmpty())
            return false;
        LivingEntity ahead = !Skills.unlocked(p, "pummel_pin") ? null : Casts.ahead(p, 3.5, 0.6);
        LivingEntity pinned = ahead != null && Grabbing.canLift(p, ahead) ? ahead : null;
        boolean pinAir = pinned != null && !pinned.onGround();
        if (pinned != null)
            pin(p, pinned);
        for (int i = 0; i < 6; i++) {
            boolean last = i == 5;
            int n = i;
            later(p, i * 2, () -> {
                if (!p.isAlive() || !BareMode.mainHand(p).isEmpty())
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
                // a punch clip is drawn on the other arm than its hand slot says, so main-fist blows use OFF
                p.swing(n % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND, true);
                CastFx.clip(p, "one_handed_punch", n % 2 == 0 ? CastFx.OFF : CastFx.MAIN, 8, 2);
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
                    if (best == pinned && pinAir)
                        launch(p, best, look);
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

    /** STAND-IN: launch speed of the final blow on a target pinned in the air (blocks per tick, before mass). */
    private static final double PIN_LAUNCH = 3.0;
    /** Ticks the pin lasts: the six blows (every 2 ticks) plus one. */
    private static final int PIN_TICKS = 11;

    /** Holds {@code t} and the puncher still in place, in the air too, until the last blow; gravity returns when the pin ends. */
    private static void pin(ServerPlayer p, LivingEntity t) {
        boolean hadGravity = !t.isNoGravity(), pHadGravity = !p.isNoGravity();
        for (int i = 0; i <= PIN_TICKS; i++) {
            boolean end = i == PIN_TICKS;
            later(p, i, () -> {
                if (end || !t.isAlive() || !p.isAlive()) {
                    if (hadGravity)
                        t.setNoGravity(false);
                    if (pHadGravity)
                        p.setNoGravity(false);
                    return;
                }
                // the puncher hangs with the target: both still until the last blow
                p.setNoGravity(true);
                p.setDeltaMovement(Vec3.ZERO);
                p.fallDistance = 0;
                p.hurtMarked = true;
                t.setNoGravity(true);
                t.setDeltaMovement(Vec3.ZERO);
                t.fallDistance = 0;
                t.hurtMarked = true;
                CombatTicker.stagger(t, 3, ParryTuning.STAGGER_SLOW);
            });
        }
    }

    /** The last blow on an airborne pin: sends it the way the player looks (down included), lighter bodies faster. */
    private static void launch(ServerPlayer p, LivingEntity t, Vec3 look) {
        t.setNoGravity(false);
        double ratio = Math.min(1.5, BodyMass.of(p) / Math.max(1f, BodyMass.of(t) + BodyMass.armour(t)));
        t.setDeltaMovement(look.scale(PIN_LAUNCH * ratio));
        t.hurtMarked = true;
    }
}
