// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.punch;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.cast.Stamina;
import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.gear.CombatGear;
import io.github.soundgoodizerfan.feedback.combat.grab.Grabbing;
import io.github.soundgoodizerfan.feedback.combat.kick.Kicking;
import io.github.soundgoodizerfan.feedback.combat.parry.Parrying;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;

/**
 * The Knuckleblaster, server side: a worn arm granting {@link PunchTuning#HEAVY_PUNCH}. A press throws a slow heavy punch (high
 * knockback by impact over the target's mass, as the kick reads it; glass breaks); a press held on after it fires the shells, a
 * shockwave that pushes creatures off and turns projectiles, harmless to the wearer. Both spend stamina and wear the arm.
 * The client only says the key went down or stayed down.
 */
public final class Punching {

    /** Game tick each player last threw a punch the shells could still follow. */
    private static final Map<UUID, Long> LAST_PUNCH = new HashMap<>();

    private Punching() {
    }

    /**
     * The hand whose arm is the left one, where the gauntlet is worn: the off hand for a right-handed player, the main hand for a
     * left-handed one. The swing is played with this hand so the right arm is never the one that comes out.
     */
    public static InteractionHand hand(net.minecraft.world.entity.player.Player player) {
        return player.getMainArm() == net.minecraft.world.entity.HumanoidArm.LEFT ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    private static ItemStack arm(ServerPlayer player) {
        return CombatGear.granting(player, PunchTuning.HEAVY_PUNCH);
    }

    /** Whether {@code player} wears a whole Knuckleblaster and is free to throw it. */
    public static boolean canPunch(ServerPlayer player) {
        return !arm(player).isEmpty() && CombatState.of(player).ready() && !player.isPassenger() && !player.isSpectator() && !player.isSleeping()
                && !Grabbing.holding(player);
    }

    private static void denied(ServerPlayer player) {
        player.playNotifySound(FSounds.PRESS_DENIED.get(), SoundSource.PLAYERS, FeelTuning.DENY_VOLUME, FeelTuning.DENY_PITCH);
    }

    public static void punch(ServerPlayer player) {
        if (!canPunch(player) || !Stamina.spend(player, PunchTuning.STAMINA)) {
            denied(player);
            return;
        }
        ServerLevel level = player.serverLevel();
        CombatState.set(player, new CombatState(CombatPhase.IDLE, PunchTuning.COOLDOWN, 0, CombatState.of(player).iFrames()));
        LAST_PUNCH.put(player.getUUID(), level.getGameTime());
        // A punch cancels a whip's reel and keeps the wearer's momentum.
        io.github.soundgoodizerfan.feedback.combat.whip.Whipping.cancel(player);
        player.swing(hand(player), true);
        Vec3 from = player.getEyePosition(), dir = player.getLookAngle();
        breakGlass(player, from, dir);
        Optional<LivingEntity> picked = Kicking.pick(player, from, dir, PunchTuning.REACH);
        Vec3 fist = from.add(dir.scale(1.1)).add(0, -0.25, 0);
        if (picked.isEmpty()) {
            CombatCues.everyone(player, FSounds.ROLL_WHOOSH.get(), FeelTuning.ROLL_VOLUME, 0.7f);
            level.sendParticles(ParticleTypes.POOF, fist.x, fist.y, fist.z, 0, dir.x, dir.y, dir.z, 0.35);
            return;
        }
        LivingEntity target = picked.get();
        float impact = ParryTuning.standInMass(player) * PunchTuning.JOULES_PER_PUNCHER_MU + PunchTuning.DAMAGE * ParryTuning.JOULES_PER_DAMAGE;
        float mass = ParryTuning.standInMass(target);
        target.hurt(player.damageSources().playerAttack(player), PunchTuning.DAMAGE);
        float knock = Mth.clamp(impact / mass * PunchTuning.KNOCK_PER_SPECIFIC_IMPACT, PunchTuning.KNOCK_MIN, PunchTuning.KNOCK_MAX);
        target.knockback(knock, -dir.x, -dir.z);
        target.hurtMarked = true;
        wear(player, PunchTuning.WEAR_PUNCH);
        CombatCues.everyone(player, FSounds.FEEL_HEAVY.get(), FeelTuning.HEAVY_VOLUME, 0.9f);
        FeelTuning.HEAVY.fire(player, target.getBoundingBox().getCenter());
        punchFx(level, target.getBoundingBox().getCenter(), dir, knock / PunchTuning.KNOCK_MAX);
    }

    /** A landed punch: a hard crack of sparks thrown along the blow, a ring of dust round the point of impact, a smoke puff. */
    private static void punchFx(ServerLevel level, Vec3 at, Vec3 dir, float power) {
        int n = 10 + Math.round(18 * Mth.clamp(power, 0f, 1f));
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, n, 0.2, 0.25, 0.2, 0.9);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 0, dir.x, dir.y + 0.1, dir.z, 0.9);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, n, 0.15, 0.2, 0.15, 0.5);
        level.sendParticles(ParticleTypes.POOF, at.x, at.y, at.z, 4, 0.2, 0.2, 0.2, 0.08);
        ring(level, ParticleTypes.CLOUD, at, dir, 10, 0.25);
    }

    /** {@code count} particles from {@code at} outward in the plane square to {@code axis}, the shape of a shock. */
    private static void ring(ServerLevel level, net.minecraft.core.particles.ParticleOptions o, Vec3 at, Vec3 axis, int count, double speed) {
        Vec3 u = axis.cross(Math.abs(axis.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
        Vec3 v = axis.cross(u).normalize();
        for (int i = 0; i < count; i++) {
            double a = i * Math.PI * 2 / count;
            Vec3 d = u.scale(Math.cos(a)).add(v.scale(Math.sin(a)));
            level.sendParticles(o, at.x, at.y, at.z, 0, d.x, d.y, d.z, speed);
        }
    }

    /** The shells going off: a flash and three rings of different speed (a front you can watch cross the radius), a spray of fire forward, smoke, and spent casings thrown back off the fist. */
    private static void shockFx(ServerLevel level, ServerPlayer player, Vec3 centre, Vec3 look) {
        level.sendParticles(ParticleTypes.FLASH, centre.x, centre.y, centre.z, 1, 0, 0, 0, 0);
        level.sendParticles(ParticleTypes.EXPLOSION, centre.x, centre.y, centre.z, 1, 0, 0, 0, 0);
        ring(level, ParticleTypes.CLOUD, centre, look, 28, 0.9);
        ring(level, ParticleTypes.CRIT, centre, look, 24, 1.3);
        ring(level, ParticleTypes.ELECTRIC_SPARK, centre, look, 20, 1.8);
        level.sendParticles(ParticleTypes.FLAME, centre.x, centre.y, centre.z, 18, 0.15, 0.15, 0.15, 0.25);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, centre.x, centre.y, centre.z, 8, 0.4, 0.4, 0.4, 0.05);
        Vec3 fist = player.getEyePosition().add(look.scale(0.9)).add(0, -0.3, 0);
        net.minecraft.core.particles.ItemParticleOption casing = new net.minecraft.core.particles.ItemParticleOption(
                ParticleTypes.ITEM, new ItemStack(net.minecraft.world.item.Items.GOLD_NUGGET));
        Vec3 side = look.cross(new Vec3(0, 1, 0)).normalize();
        for (int i = 0; i < 4; i++)
            level.sendParticles(casing, fist.x, fist.y, fist.z, 0, side.x * 0.6 + (level.random.nextDouble() - 0.5) * 0.3,
                    0.5 + level.random.nextDouble() * 0.3, side.z * 0.6 + (level.random.nextDouble() - 0.5) * 0.3, 0.5);
    }

    /** The shells: only right after a punch of the wearer's own, once. */
    public static void shockwave(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Long punched = LAST_PUNCH.get(player.getUUID());
        if (punched == null || level.getGameTime() - punched > PunchTuning.SHOCK_WINDOW_TICKS || arm(player).isEmpty() || player.isSpectator())
            return;
        if (!Stamina.spend(player, PunchTuning.SHOCK_STAMINA)) {
            denied(player);
            return;
        }
        LAST_PUNCH.remove(player.getUUID());
        CombatState.set(player, new CombatState(CombatPhase.IDLE, PunchTuning.SHOCK_COOLDOWN, 0, CombatState.of(player).iFrames()));
        Vec3 look = player.getLookAngle();
        Vec3 centre = player.getEyePosition().add(look.scale(PunchTuning.SHOCK_AHEAD));
        AABB box = new AABB(centre, centre).inflate(PunchTuning.SHOCK_RADIUS);
        float impact = ParryTuning.standInMass(player) * PunchTuning.JOULES_PER_PUNCHER_MU;
        for (Entity e : level.getEntities(player, box, x -> x.isAlive() && !x.isSpectator() && x.position().distanceToSqr(centre) <= PunchTuning.SHOCK_RADIUS * PunchTuning.SHOCK_RADIUS)) {
            if (e instanceof Projectile projectile) {
                if (projectile.getOwner() != player)
                    Parrying.turn(player, projectile, Math.max(0.5f, (float) projectile.getDeltaMovement().length()) * PunchTuning.SHOCK_SPEED);
            } else if (e instanceof LivingEntity target) {
                Vec3 away = target.position().subtract(player.position());
                Vec3 flat = new Vec3(away.x, 0, away.z);
                flat = flat.lengthSqr() < 1e-6 ? new Vec3(look.x, 0, look.z) : flat.normalize();
                target.hurt(player.damageSources().playerAttack(player), PunchTuning.SHOCK_DAMAGE);
                float mass = ParryTuning.standInMass(target);
                target.knockback(Mth.clamp(impact / mass * PunchTuning.KNOCK_PER_SPECIFIC_IMPACT * PunchTuning.SHOCK_KNOCK, PunchTuning.KNOCK_MIN, PunchTuning.KNOCK_MAX), -flat.x, -flat.z);
                if (impact / mass > 1f)
                    target.setDeltaMovement(target.getDeltaMovement().add(0, PunchTuning.SHOCK_POP, 0));
                target.hurtMarked = true;
            }
        }
        breakGlass(player, player.getEyePosition(), look);
        shockFx(level, player, centre, look);
        wear(player, PunchTuning.WEAR_SHOCK);
        CombatCues.everyone(player, FSounds.FEEL_HEAVY.get(), FeelTuning.HEAVY_VOLUME, 0.6f);
        FeelTuning.HEAVY.fire(player, centre);
    }

    /** Glass, found by tag, in reach of the fist breaks (as a blow would, where the world lets the player). */
    private static void breakGlass(ServerPlayer player, Vec3 from, Vec3 dir) {
        ServerLevel level = player.serverLevel();
        var hit = level.clip(new ClipContext(from, from.add(dir.scale(PunchTuning.REACH)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (hit.getType() != HitResult.Type.BLOCK)
            return;
        BlockPos pos = hit.getBlockPos();
        BlockState state = level.getBlockState(pos);
        if ((state.is(Tags.Blocks.GLASS_BLOCKS) || state.is(Tags.Blocks.GLASS_PANES)) && level.mayInteract(player, pos))
            level.destroyBlock(pos, true, player);
    }

    private static void wear(ServerPlayer player, int points) {
        ItemStack arm = arm(player);
        if (!arm.isEmpty())
            arm.hurtAndBreak(points, player, EquipmentSlot.MAINHAND);
    }
}
