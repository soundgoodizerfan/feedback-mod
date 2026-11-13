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
 * What the module abilities do in the world. A pulse is one act with a cost in Eu, a cooldown and a reach that grows with the
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
            case HEAL -> SuitTuning.HEAL_COOLDOWN;
            case VENT -> SuitTuning.VENT_COOLDOWN;
            case BURST -> SuitTuning.BURST_COOLDOWN;
            default -> SuitTuning.EMP_COOLDOWN;
        };
    }

    /** Eu {@code a} costs to fire. */
    static float cost(Ability a) {
        return switch (a) {
            case SHOCK -> SuitTuning.SHOCK_COST;
            case SCAN -> SuitTuning.SCAN_COST;
            case HEAL -> SuitTuning.HEAL_COST;
            case VENT -> SuitTuning.VENT_COST;
            case BURST -> SuitTuning.BURST_COST;
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
            case BURST -> {
                float density = Math.min(1f, io.github.soundgoodizerfan.feedback.vehicle.AirDensityTable.at(level, p.getY()) / SuitTuning.REF_AIR_DENSITY);
                Vec3 look = p.getLookAngle();
                p.setDeltaMovement(p.getDeltaMovement().add(look.x * SuitTuning.BURST_PUSH * power * density, SuitTuning.BURST_LIFT * density,
                        look.z * SuitTuning.BURST_PUSH * power * density));
                p.hurtMarked = true;
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME, p.getX(), p.getY() + 0.5, p.getZ(), 15, 0.2, 0.2, 0.2, 0.05);
                level.playSound(null, p.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.8f, 1f);
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

    /**
     * Doses the wearer from a bottle of brewed potion in the pack, the effect drinking it would give (a dud or a flat one gives nothing and is
     * kept), its length scaled by {@code scale}: the dispenser meters a dose, but a bottle is spent whole. Returns whether one was dosed.
     */
    static boolean dose(ServerPlayer p, float scale) {
        for (net.minecraft.world.item.ItemStack s : p.getInventory().items) {
            if (!io.github.soundgoodizerfan.feedback.potion.PotionItem.dose(p, s, scale))
                continue;
            s.shrink(1);
            net.minecraft.world.item.ItemStack bottle = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GLASS_BOTTLE);
            if (!p.getInventory().add(bottle))
                p.drop(bottle, false);
            p.serverLevel().playSound(null, p.blockPosition(), SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.8f, 1.4f);
            return true;
        }
        return false;
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
