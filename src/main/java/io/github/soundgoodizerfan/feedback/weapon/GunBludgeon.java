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
        io.github.soundgoodizerfan.feedback.combat.skill.Rearms.on(player, io.github.soundgoodizerfan.feedback.combat.skill.SkillTree.Rearm.STAGGER);
        Vec3 d = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        float yaw = (float) (Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (-Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
        GunShots.triggerHeard(player.serverLevel(), player, gun, yaw, pitch, System.currentTimeMillis(), false, hand);
    }

    /** The target and the game tick until which a shot at it counts as Muzzle Stagger's follow-up. */
    public record Whip(UUID target, long until) {
    }

    private static final Map<UUID, Whip> WHIPPED = new HashMap<>();

    /** The whip {@code shooter} landed lately, if still live. */
    public static Whip whip(LivingEntity shooter) {
        Whip w = WHIPPED.get(shooter.getUUID());
        return w != null && shooter.level().getGameTime() <= w.until() ? w : null;
    }

    /** Bayonet Drill's blow: the bayonet part's own material hardness sets it, within bounds. */
    private static float bayonetDamage(ItemStack gun, GunFigures f) {
        var design = io.github.soundgoodizerfan.feedback.modular.ModularStacks.design(gun);
        var part = design == null ? null : design.part("a");
        var mat = part == null ? null : io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials().get(part.material());
        float hard = mat == null ? 1f : mat.hardness().value();
        return Mth.clamp(hard * GunFuTuning.BAYONET_DAMAGE_PER_HARDNESS, GunFuTuning.BAYONET_DAMAGE_MIN, GunFuTuning.BAYONET_DAMAGE_MAX);
    }

    /** Brand: a whip with the barrel above the burn line sears the target by that temperature, and the barrel gives the heat up. */
    private static void brand(ServerPlayer player, ItemStack gun, LivingEntity target) {
        if (!Skills.unlocked(player, "gun_fu_brand"))
            return;
        var tu = GunCondition.temperature(gun, player.level());
        target.invulnerableTime = 0; // the sear is its own blow, not part of the whip
        if (io.github.soundgoodizerfan.feedback.core.hazard.HotContact.burn(target, tu)) {
            gun.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.TEMPERATURE.get(),
                    Math.max(0f, tu.value() - GunFuTuning.BRAND_COOL_TU));
            gun.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.HEATED_AT.get(), player.level().getGameTime());
        }
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
        // Bayonet Drill: with a bayonet lug fitted the swing is a thrust that rides the gun's own cycle.
        boolean thrust = Skills.unlocked(player, "gun_fu_bayonet") && GunSpec.of(gun).map(g -> g.has("bayonet")).orElse(false);
        float dmg = thrust ? bayonetDamage(gun, f) : damage(f);
        boolean spin = player.isShiftKeyDown() && Skills.unlocked(player, "gun_fu_spin");
        long now = player.level().getGameTime();
        long[] next = NEXT_TICK.computeIfAbsent(player.getUUID(), u -> new long[2]);
        int i = offHand ? 1 : 0;
        // One tick of grace for network jitter.
        if (now + 1 < next[i])
            return;
        int base = thrust ? Math.max(GunFuTuning.BAYONET_MIN_TICKS, Math.round(f.cycleMs() / 50f)) : cooldownTicks(f);
        next[i] = now + Math.round(base * (spin ? FTuning.GUN_SPIN_COOLDOWN_MULT : 1f));

        player.swing(hand, true);
        // Spin (skill unlock): a sneaking swing comes all the way round and takes every body within reach, not the nearest on a line.
        List<LivingEntity> targets = spin
                ? player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(FTuning.GUN_SPIN_RADIUS),
                        e -> e != player && e.isAlive() && !e.isSpectator() && !player.isAlliedTo(e))
                : SweptVolume.targets(player, FTuning.GUN_BLUDGEON_HALF_WIDTH).stream().limit(1).toList();
        var sound = SoundEvents.PLAYER_ATTACK_NODAMAGE;
        for (LivingEntity target : targets) {
            if (!target.hurt(player.damageSources().playerAttack(player), dmg))
                continue;
            brand(player, gun, target);
            if (Skills.unlocked(player, "gun_fu_muzzle_stagger"))
                WHIPPED.put(player.getUUID(), new Whip(target.getUUID(), now + GunFuTuning.MUZZLE_TICKS));
            float yaw = player.getYRot() * Mth.DEG_TO_RAD;
            double dx = spin ? target.getX() - player.getX() : Mth.sin(yaw), dz = spin ? target.getZ() - player.getZ() : -Mth.cos(yaw);
            target.knockback(FTuning.GUN_BLUDGEON_KNOCKBACK_BASE + FTuning.GUN_BLUDGEON_KNOCKBACK_PER_MU * f.gunMass().value(), dx, dz);
            sound = f.gunMass().value() >= FTuning.GUN_BLUDGEON_HEAVY_MU ? SoundEvents.PLAYER_ATTACK_STRONG : SoundEvents.PLAYER_ATTACK_WEAK;
            freeShot(player, hand, gun, target, dmg);
        }
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), sound, player.getSoundSource(), 1f, 1f);
    }
}
