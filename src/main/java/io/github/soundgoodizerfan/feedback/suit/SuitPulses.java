// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.suit;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * What the module abilities do in the world. A pulse is one act with a cost in amp-ticks, a cooldown and a reach that grows with the
 * module's power (its core's material); the cloak and the jet are worked every tick by {@link SuitServer}. Every figure is in {@link SuitTuning}.
 */
final class SuitPulses {

    private SuitPulses() {
    }

    /** Whether {@code e} is something a pulse acts on: alive, not the wearer, not a pet the wearer owns. */
    private static boolean target(ServerPlayer p, LivingEntity e) {
        return e != p && e.isAlive() && !(e instanceof OwnableEntity o && o.getOwnerUUID() != null && o.getOwnerUUID().equals(p.getUUID()));
    }

    /** Ticks before {@code a} may fire again. */
    static int cooldown(Ability a) {
        return switch (a) {
            case SHOCK -> SuitTuning.SHOCK_COOLDOWN;
            case SCAN -> SuitTuning.SCAN_COOLDOWN;
            default -> SuitTuning.EMP_COOLDOWN;
        };
    }

    /** Amp-ticks {@code a} costs to fire. */
    static float cost(Ability a) {
        return switch (a) {
            case SHOCK -> SuitTuning.SHOCK_COST;
            case SCAN -> SuitTuning.SCAN_COST;
            default -> SuitTuning.EMP_COST;
        };
    }

    /** Fires {@code a} at the wearer's feet with the module's {@code power}. */
    static void fire(ServerPlayer p, Ability a, float power) {
        var level = p.serverLevel();
        switch (a) {
            case SHOCK -> {
                double r = SuitTuning.SHOCK_RADIUS * (0.6 + 0.4 * power);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), x -> target(p, x))) {
                    Vec3 away = e.position().subtract(p.position());
                    double d = Math.max(0.5, away.length());
                    if (d > r)
                        continue;
                    e.hurt(p.damageSources().playerAttack(p), SuitTuning.SHOCK_DAMAGE * power * (float) (1 - d / (r * 1.5)));
                    Vec3 push = away.normalize().scale(SuitTuning.SHOCK_PUSH * power);
                    e.push(push.x, 0.35, push.z);
                    e.hurtMarked = true;
                }
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.EXPLOSION, p.getX(), p.getY() + 0.2, p.getZ(), 1, 0, 0, 0, 0);
                level.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 1.4f);
            }
            case SCAN -> {
                double r = SuitTuning.SCAN_RADIUS * (0.5 + 0.5 * power);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), x -> target(p, x)))
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, SuitTuning.SCAN_GLOW_TICKS, 0, true, false, false));
                level.playSound(null, p.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7f, 1.8f);
            }
            default -> {
                double r = SuitTuning.EMP_RADIUS * (0.6 + 0.4 * power);
                int ticks = Math.round(SuitTuning.EMP_TICKS * power);
                for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(r), x -> target(p, x))) {
                    if (p.distanceToSqr(e) > r * r)
                        continue;
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2, true, true));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1, true, true));
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks / 2, 0, true, true));
                    if (e instanceof Mob m)
                        m.setTarget(null);
                }
                level.playSound(null, p.blockPosition(), SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.5f, 2f);
            }
        }
    }

    /** Held cloak: keeps a hidden, ambient invisibility on the wearer. */
    static void cloak(ServerPlayer p) {
        if (p.tickCount % SuitTuning.CLOAK_REFRESH == 0)
            p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, SuitTuning.CLOAK_TICKS, 0, true, false, false));
    }

    /** Takes off the invisibility the cloak lent, leaving a longer or visible one (a potion's) alone. */
    static void dropCloak(ServerPlayer p) {
        var inst = p.getEffect(MobEffects.INVISIBILITY);
        if (inst != null && inst.isAmbient() && !inst.isVisible() && inst.getDuration() <= SuitTuning.CLOAK_TICKS)
            p.removeEffect(MobEffects.INVISIBILITY);
    }

    /** Held jet: lifts the wearer, thinned by the air, up to a climb rate. Returns the share of full thrust it gave (0 in no air). */
    static float jet(ServerPlayer p, float power, float density) {
        if (density <= 0.05f)
            return 0f;
        Vec3 v = p.getDeltaMovement();
        double top = SuitTuning.JET_TOP * power;
        double ny = Math.min(Math.max(v.y, -0.2) + SuitTuning.JET_ACCEL * power * density, Math.max(top, v.y));
        p.setDeltaMovement(v.x, ny, v.z);
        p.fallDistance = 0f;
        p.hurtMarked = true;
        return density;
    }
}
