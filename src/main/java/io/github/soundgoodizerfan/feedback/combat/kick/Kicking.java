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
package io.github.soundgoodizerfan.feedback.combat.kick;

import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.grab.Grabbing;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.registry.FSounds;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * The kick: a short, low, forward blow (implementation plan 0, "Kick"; ruled: both). Against a target
 * with a raised shield it breaks the guard and staggers; against anything else it shoves, and staggers
 * only if the shove is large against the target's mass. The same impact over mass reads both ways,
 * the same stand-in mass the parry's stagger uses, so a heavy target resists the kick as it resists
 * a blow. There is no sprint kick (user ruling). Server only; the client sends the press.
 */
public final class Kicking {

    private Kicking() {
    }

    /** Whether {@code player} may kick now: free to act, so not mid-parry, mid-roll, staggered or on cooldown, and not holding someone. */
    public static boolean canKick(Player player) {
        return CombatState.of(player).ready() && !player.isPassenger() && !player.isSpectator() && !player.isSleeping()
                && !Grabbing.holding(player);
    }

    /** The nearest living thing a ray of {@code reach} blocks from {@code from} along {@code dir} touches, its box widened by {@link KickTuning#GRACE}. */
    public static Optional<LivingEntity> pick(Player player, Vec3 from, Vec3 dir, double reach) {
        Vec3 end = from.add(dir.scale(reach));
        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Entity e : player.level().getEntities(player, new AABB(from, end).inflate(KickTuning.GRACE + 0.5),
                x -> x instanceof LivingEntity l && l.isAlive() && !l.isSpectator())) {
            Optional<Vec3> hit = e.getBoundingBox().inflate(KickTuning.GRACE).clip(from, end);
            if (hit.isPresent() && hit.get().distanceToSqr(from) < bestDist) {
                bestDist = hit.get().distanceToSqr(from);
                best = (LivingEntity) e;
            }
        }
        return Optional.ofNullable(best);
    }

    /** Runs a kick press for {@code player}: the verdict, the cost and the blow. */
    public static void kick(ServerPlayer player) {
        if (!canKick(player)) {
            player.playNotifySound(FSounds.PRESS_DENIED.get(), SoundSource.PLAYERS, FeelTuning.DENY_VOLUME, FeelTuning.DENY_PITCH);
            return;
        }
        CombatState.set(player, new CombatState(CombatPhase.KICK, KickTuning.SWING, KickTuning.COOLDOWN - KickTuning.SWING, CombatState.of(player).iFrames()));
        player.causeFoodExhaustion(KickTuning.EXHAUSTION);
        Vec3 look = player.getLookAngle();
        Vec3 dir = new Vec3(look.x, 0, look.z);
        dir = dir.lengthSqr() < 1e-6 ? Vec3.directionFromRotation(0, player.getYRot()) : dir.normalize();
        Optional<LivingEntity> picked = pick(player, player.position().add(0, KickTuning.WAIST, 0), dir, KickTuning.REACH);
        if (picked.isEmpty()) {
            CombatCues.everyone(player, FSounds.ROLL_WHOOSH.get(), FeelTuning.ROLL_VOLUME, 1.4f);
            return;
        }
        LivingEntity target = picked.get();
        float impact = ParryTuning.standInMass(player) * KickTuning.JOULES_PER_KICKER_MU + KickTuning.DAMAGE * ParryTuning.JOULES_PER_DAMAGE;
        float mass = ParryTuning.standInMass(target);
        boolean guard = target.isBlocking();
        if (guard)
            breakGuard(target, Mth.clamp(Math.round(impact / mass * KickTuning.DISABLE_PER_SPECIFIC_IMPACT), KickTuning.DISABLE_MIN, KickTuning.DISABLE_MAX));
        target.hurt(player.damageSources().playerAttack(player), KickTuning.DAMAGE);
        float knock = Mth.clamp(impact / mass * KickTuning.KNOCK_PER_SPECIFIC_IMPACT, KickTuning.KNOCK_MIN, KickTuning.KNOCK_MAX);
        target.knockback(knock, -dir.x, -dir.z);
        target.hurtMarked = true;
        if (guard)
            CombatTicker.stagger(target, Math.max(KickTuning.GUARD_STAGGER_MIN, ParryTuning.staggerTicks(impact, mass)), ParryTuning.STAGGER_SLOW);
        else if (ParryTuning.rawStaggerTicks(impact, mass) >= KickTuning.STAGGER_MIN_RAW)
            CombatTicker.stagger(target, ParryTuning.staggerTicks(impact, mass), ParryTuning.STAGGER_SLOW);
        (guard ? FeelTuning.HEAVY : FeelTuning.LIGHT).fire(player, target.getBoundingBox().getCenter());
    }

    /** Drops {@code target}'s guard and keeps it down for {@code ticks}: a player's shield goes on cooldown; a mob only lowers it. */
    private static void breakGuard(LivingEntity target, int ticks) {
        if (target instanceof Player p)
            p.getCooldowns().addCooldown(p.getUseItem().getItem(), ticks);
        target.stopUsingItem();
        target.level().broadcastEntityEvent(target, (byte) 30);
    }
}
