// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Suppressing fire, a held cast: while the key is down the gun fires a round every few ticks, each laid to one side or the
 * other of the aim line and sweeping back and forth across a cone that follows where you look, never on a target. Pin down
 * slows whatever a round passes close to, so the sweep denies the ground. Every round is a real round (ammo, heat, report,
 * the gun's own cycle, which may refuse a beat); stamina drains each tick, and it ends on an empty gun, an empty bar or
 * letting go.
 */
final class SuppressingFire {

    private SuppressingFire() {
    }

    /** STAND-INS ([OPEN]): ticks between rounds, half angle swept in degrees, ticks for one pass across, stamina a tick, longest hold in ticks. */
    private static final int GAP = 3, SWEEP = 12, LIMIT = 200;
    private static final float HALF_DEG = 18f;
    static final float DRAIN = 0.3f;

    /** One burst under way: the tick it began. */
    private static final Map<UUID, Long> BURSTS = new HashMap<>();

    static final Casts.Hold HOLD = new Casts.Hold(DRAIN, SuppressingFire::tick, SuppressingFire::stop);

    /** Suppressing fire: starts the sweep with a loaded gun in hand. */
    static boolean act(ServerPlayer p) {
        ItemStack gun = p.getMainHandItem();
        if (!(gun.getItem() instanceof GunItem) || GunAmmo.count(gun) < 1)
            return false;
        BURSTS.put(p.getUUID(), p.level().getGameTime());
        CastFx.sound(p.serverLevel(), p.getEyePosition(), SoundEvents.CROSSBOW_LOADING_END.value(), 0.8f, 0.8f);
        return true;
    }

    /** Each tick of the hold: a round on every {@code GAP}th, swept across the cone; false once the burst has ended itself. */
    static boolean tick(ServerPlayer p) {
        Long start = BURSTS.get(p.getUUID());
        ItemStack gun = p.getMainHandItem();
        long age = start == null ? 0 : p.level().getGameTime() - start;
        if (start == null || !p.isAlive() || !(gun.getItem() instanceof GunItem) || GunAmmo.count(gun) < 1 || age > LIMIT) {
            BURSTS.remove(p.getUUID());
            return false;
        }
        if (age % GAP != 0)
            return true;
        // A triangle wave across the cone: out to one side, back past the aim line, out to the other.
        float phase = (float) (age % (2L * SWEEP)) / SWEEP;
        float side = phase < 1f ? phase * 2f - 1f : 3f - phase * 2f;
        float yaw = p.getYRot() + HALF_DEG * Mth.clamp(side, -1f, 1f);
        Vec3 muzzle = p.getEyePosition().add(p.getLookAngle().scale(0.9));
        p.serverLevel().sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 2, 0.05, 0.05, 0.05, 0.04);
        GunShots.triggerHeard(p.serverLevel(), p, gun, yaw, p.getXRot(), System.currentTimeMillis(), false, InteractionHand.MAIN_HAND);
        return true;
    }

    /** The key is let go, or the bar runs dry. */
    static void stop(ServerPlayer p) {
        BURSTS.remove(p.getUUID());
    }
}
