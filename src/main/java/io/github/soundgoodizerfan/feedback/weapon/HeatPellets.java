// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.gear.CombatGear;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.registry.FCombat;
import io.github.soundgoodizerfan.feedback.combat.parry.Parrying;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * The slow heat of a core breech: the one shot that is a projectile, not a beam. Each pellet is a point stepped every tick
 * (no entity), so a Feedbacker punch can find the wielder's own pellets within reach and send them back fast and true,
 * the way it sends a coin up ({@link CoinShot#punch}). A pellet is spent on the first body or block it meets, lights a body
 * it hits, and dies at the end of its short range. A body whose Feedbacker is in its parry window punches a pellet back
 * through {@link Parrying#catchBullet}.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class HeatPellets {

    private static final class Pellet {
        final ServerLevel level;
        final LivingEntity owner;
        Vec3 pos, vel;
        float damage;
        int life;
        boolean returned;
        final int id = NEXT_ID--, rgb;

        Pellet(ServerLevel level, LivingEntity owner, Vec3 pos, Vec3 vel, float damage, int life, int rgb) {
            this.rgb = rgb;
            this.level = level;
            this.owner = owner;
            this.pos = pos;
            this.vel = vel;
            this.damage = damage;
            this.life = life;
        }
    }

    private static final List<Pellet> PELLETS = new ArrayList<>();
    /** Ids for the clients' blobs: negative, so they never meet a bullet's. */
    private static int NEXT_ID = -1;

    private HeatPellets() {
    }

    /**
     * Throws {@code beams} pellets from {@code shooter}'s eye within a cone of {@code coneDeg}, sharing {@code joules}
     * between them (a splitter's share), each flying {@code range} blocks at most.
     */
    public static void launch(ServerLevel level, LivingEntity shooter, float joules, float range, int beams, float coneDeg, float yaw, float pitch, int rgb) {
        int n = Math.max(1, beams);
        float each = n > 1 ? joules * ModularTuning.EGUN_SPLIT_TOTAL / n : joules;
        int life = Math.max(1, Math.round(range / ModularTuning.EGUN_CORE_SPEED));
        Vec3 eye = shooter.getEyePosition();
        for (int i = 0; i < n; i++) {
            float r = coneDeg * Mth.sqrt(level.random.nextFloat()), theta = level.random.nextFloat() * Mth.TWO_PI;
            Vec3 dir = Vec3.directionFromRotation(Mth.clamp(pitch + r * Mth.sin(theta), -90f, 90f), yaw + r * Mth.cos(theta));
            Pellet s = new Pellet(level, shooter, eye.add(dir.scale(0.4)), dir.scale(ModularTuning.EGUN_CORE_SPEED),
                    each * ModularTuning.EGUN_DAMAGE_PER_J, life, rgb);
            PELLETS.add(s);
            fly(s, false);
        }
        level.playSound(null, shooter.blockPosition(), io.github.soundgoodizerfan.feedback.registry.FSounds.GUN_FLAME.get(), SoundSource.PLAYERS, 1f, 0.9f + level.random.nextFloat() * 0.2f);
    }

    /**
     * A Feedbacker punch (the press with the gauntlet worn): the puncher's own pellets within reach and in front are
     * sent back along the look, faster and harder, and are not parried again.
     */
    public static void punch(net.minecraft.server.level.ServerPlayer p) {
        if (bounceAll(p))
            Parrying.punchedOwn(p.serverLevel(), p, ModularTuning.EGUN_CORE_PELLET_MU, ModularTuning.EGUN_CORE_SPEED);
    }

    /** Sends every pellet of {@code owner} within reach back; whether any went. One parry per punch, however many pellets. */
    private static boolean bounceAll(LivingEntity owner) {
        boolean any = false;
        for (Pellet s : PELLETS)
            any |= bounce(s, owner);
        return any;
    }

    /** Sends {@code s} back along {@code p}'s look if it is p's own, not yet returned, within reach and in front; whether it did. */
    private static boolean bounce(Pellet s, LivingEntity p) {
        Vec3 look = p.getLookAngle(), to = s.pos.subtract(p.getEyePosition());
        if (s.owner != p || s.returned || to.length() > ModularTuning.EGUN_CORE_PARRY_RANGE || to.normalize().dot(look) < 0.3)
            return false;
        s.returned = true;
        s.vel = look.scale(ModularTuning.EGUN_CORE_SPEED * ParryTuning.FEEDBACKER_SPEED);
        s.damage *= ParryTuning.FEEDBACKER_SPEED;
        s.life = Math.max(s.life, 12);
        fly(s, false);
        return true;
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (PELLETS.isEmpty())
            return;
        // A Feedbacker held in its parry window catches the wielder's own pellets as they pass, all of them in one parry.
        java.util.Set<LivingEntity> caught = new java.util.HashSet<>();
        for (Pellet s : new ArrayList<>(PELLETS))
            if (!s.returned && !caught.contains(s.owner) && inWindow(s.owner))
                caught.add(s.owner);
        for (LivingEntity owner : caught)
            if (bounceAll(owner) && owner.level() instanceof ServerLevel level)
                Parrying.punchedOwn(level, owner, ModularTuning.EGUN_CORE_PELLET_MU, ModularTuning.EGUN_CORE_SPEED);
        PELLETS.removeIf(s -> {
            if (!step(s))
                return false;
            fly(s, true);
            return true;
        });
    }

    @SubscribeEvent
    public static void onStopped(ServerStoppedEvent event) {
        PELLETS.clear();
    }

    private static boolean inWindow(LivingEntity e) {
        return e.hasData(FCombat.STATE) && CombatState.of(e).phase() == CombatPhase.PARRY_ACTIVE
                && !CombatGear.granting(e, ParryTuning.PUNCH_PROJECTILES).isEmpty();
    }

    /** Tells the clients where a blob is and how it flies, or that it is spent. */
    private static void fly(Pellet s, boolean ended) {
        io.github.soundgoodizerfan.feedback.net.GunTracerPayload.send(s.level, s.id, ended, s.pos, s.vel, 0xFF000000 | s.rgb, 0f, EnergyStyle.PLASMA);
    }

    /** One tick of one pellet; true when it is spent. */
    private static boolean step(Pellet s) {
        ServerLevel level = s.level;
        if (--s.life < 0 || s.owner.isRemoved())
            return true;
        Vec3 from = s.pos, to = from.add(s.vel);
        var block = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        Vec3 end = block.getType() == HitResult.Type.MISS ? to : block.getLocation();
        Entity best = null;
        double bestD = Double.MAX_VALUE;
        for (Entity e : level.getEntities(s.owner, new AABB(from, end).inflate(1.0), e -> e.isPickable() && !e.isSpectator() && e.isAlive())) {
            var at = e.getBoundingBox().inflate(e.getPickRadius() + 0.05).clip(from, end);
            if (at.isPresent() && at.get().distanceTo(from) < bestD) {
                bestD = at.get().distanceTo(from);
                best = e;
            }
        }
        if (best != null) {
            if (!s.returned && best instanceof LivingEntity catcher
                    && Parrying.catchBullet(level, s.owner, catcher, ModularTuning.EGUN_CORE_PELLET_MU, (float) s.vel.length(), s.damage))
                return true;
            s.pos = end;
            best.invulnerableTime = 0;
            Entity target = best;
            GunShots.asBullet(() -> target.hurt(GunShots.attack(level, s.owner), s.damage));
            target.igniteForSeconds(3f);
            GunImpacts.energyBody(level, end, s.vel.normalize(), 1f, s.rgb, EnergyStyle.PLASMA);
            return true;
        }
        if (block.getType() != HitResult.Type.MISS) {
            s.pos = end;
            GunImpacts.energyBlock(level, block.getBlockPos(), block.getDirection(), end, 1f, s.rgb, EnergyStyle.PLASMA);
            return true;
        }
        s.pos = to;
        return false;
    }
}
