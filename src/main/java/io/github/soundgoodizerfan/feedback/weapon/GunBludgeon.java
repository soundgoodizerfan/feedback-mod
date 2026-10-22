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
package io.github.soundgoodizerfan.feedback.weapon;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.hit.SweptVolume;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * A gun swung as a bludgeon (gun-fu, the melee stance). Every figure is the gun's baked mass
 * ({@link GunFigures#gunMass()}); no gun has a row of its own. Server side it resolves one swing: a short
 * line from the eye ({@link SweptVolume#targets}), the nearest living thing on it takes a normal
 * player-attack {@code hurt}, so the damage event and every trait and perk resolve as for any hit
 * (traits read the main hand, so an off-hand swing carries the main hand's traits until the per-hand
 * work lands). Skill-gated moves: the swing is {@code gun_fu_pistol_whip}, the off-hand swing {@code gun_fu_dual},
 * the free shot {@code gun_fu_chamber}, a sneaking all-round swing {@code gun_fu_spin}. A plain swing wears nothing: misuse wears, a swing is not misuse.
 */
public final class GunBludgeon {

    /** Earliest game tick each player's hand may swing again; [main, off]. */
    private static final Map<UUID, long[]> NEXT_TICK = new HashMap<>();

    private GunBludgeon() {
    }

    /** Damage of a swing of a gun of this baked mass. */
    public static float damage(GunFigures f) {
        return Math.min(FTuning.GUN_BLUDGEON_DAMAGE_MAX,
                FTuning.GUN_BLUDGEON_DAMAGE_BASE + FTuning.GUN_BLUDGEON_DAMAGE_PER_MU * f.gunMass().value());
    }

    /** Ticks before the same hand can swing again: a heavy gun is slow to bring round. */
    public static int cooldownTicks(GunFigures f) {
        return Mth.clamp(Math.round(FTuning.GUN_BLUDGEON_TICKS_PER_MU * f.gunMass().value()),
                FTuning.GUN_BLUDGEON_TICKS_MIN, FTuning.GUN_BLUDGEON_TICKS_MAX);
    }

    /**
     * A hit whose impact over the target's stand-in mass staggers it (the parry's formula, shared through
     * {@link ParryTuning}) chambers one free shot at it: the gun's normal trigger path, so the round comes
     * from the chambered ammo, the hand's own cycle, jam and condition rules apply, and an empty or spent gun
     * simply does not fire. Per hand, since the shot books that hand's cycle. A target already staggered or
     * rolling is not staggered again, so one stagger buys one shot.
     */
    private static void freeShot(ServerPlayer player, InteractionHand hand, ItemStack gun, LivingEntity target, float damage) {
        if (!Skills.unlocked(player, "gun_fu_chamber"))
            return;
        float raw = ParryTuning.rawStaggerTicks(damage * ParryTuning.JOULES_PER_DAMAGE, ParryTuning.standInMass(target));
        if (raw < FTuning.GUN_BLUDGEON_STAGGER_MIN_TICKS || CombatState.of(target).phase() == CombatPhase.STAGGER)
            return;
        CombatTicker.stagger(target, ParryTuning.staggerTicks(damage * ParryTuning.JOULES_PER_DAMAGE, ParryTuning.standInMass(target)),
                ParryTuning.STAGGER_SLOW);
        if (CombatState.of(target).phase() != CombatPhase.STAGGER)
            return; // a rolling target slipped it
        Vec3 d = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        float yaw = (float) (Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (-Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
        GunShots.trigger(player.serverLevel(), player, gun, yaw, pitch, System.currentTimeMillis(), false, hand);
    }

    public static void swing(ServerPlayer player, boolean offHand) {
        InteractionHand hand = offHand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack gun = player.getItemInHand(hand);
        GunFigures f = gun.getItem() instanceof GunItem ? GunItem.figures(gun) : null;
        if (f == null || (offHand && !GunItem.offHandFires(player)))
            return;
        // The skill tree gates the moves: the swing itself is pistol_whip, the off-hand swing is dual.
        if (!Skills.unlocked(player, "gun_fu_pistol_whip") || offHand && !Skills.unlocked(player, "gun_fu_dual")) {
            player.playNotifySound(FSounds.PRESS_DENIED.get(), SoundSource.PLAYERS, FeelTuning.DENY_VOLUME, FeelTuning.DENY_PITCH);
            return;
        }
        boolean spin = player.isShiftKeyDown() && Skills.unlocked(player, "gun_fu_spin");
        long now = player.level().getGameTime();
        long[] next = NEXT_TICK.computeIfAbsent(player.getUUID(), u -> new long[2]);
        int i = offHand ? 1 : 0;
        // One tick of grace for network jitter.
        if (now + 1 < next[i])
            return;
        next[i] = now + Math.round(cooldownTicks(f) * (spin ? FTuning.GUN_SPIN_COOLDOWN_MULT : 1f));

        player.swing(hand, true);
        // Spin (skill unlock): a sneaking swing comes all the way round and takes every body within reach, not the nearest on a line.
        List<LivingEntity> targets = spin
                ? player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(FTuning.GUN_SPIN_RADIUS),
                        e -> e != player && e.isAlive() && !e.isSpectator() && !player.isAlliedTo(e))
                : SweptVolume.targets(player, FTuning.GUN_BLUDGEON_HALF_WIDTH).stream().limit(1).toList();
        var sound = SoundEvents.PLAYER_ATTACK_NODAMAGE;
        for (LivingEntity target : targets) {
            if (!target.hurt(player.damageSources().playerAttack(player), damage(f)))
                continue;
            float yaw = player.getYRot() * Mth.DEG_TO_RAD;
            double dx = spin ? target.getX() - player.getX() : Mth.sin(yaw), dz = spin ? target.getZ() - player.getZ() : -Mth.cos(yaw);
            target.knockback(FTuning.GUN_BLUDGEON_KNOCKBACK_BASE + FTuning.GUN_BLUDGEON_KNOCKBACK_PER_MU * f.gunMass().value(), dx, dz);
            sound = f.gunMass().value() >= FTuning.GUN_BLUDGEON_HEAVY_MU ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_WEAK;
            freeShot(player, hand, gun, target, damage(f));
        }
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), sound, player.getSoundSource(), 1f, 1f);
    }
}
