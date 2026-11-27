// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import java.util.Comparator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.later;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;

/** Walking Fire: three rounds laid on the next targets in front. */
final class WalkingFire {

    private WalkingFire() {
    }

    /** Walking fire: three rounds, each laid on the next target in front, so a sweep of the room is one press. */
    static boolean act(ServerPlayer p) {
        ItemStack gun = p.getMainHandItem();
        if (!(gun.getItem() instanceof GunItem) || GunAmmo.count(gun) < 1)
            return false;
        for (int i = 0; i < 3; i++) {
            int n = i;
            later(p, n * 4, () -> {
                ItemStack held = p.getMainHandItem();
                if (!p.isAlive() || !(held.getItem() instanceof GunItem) || GunAmmo.count(held) < 1)
                    return;
                List<LivingEntity> seen = near(p, p.getEyePosition().add(p.getLookAngle().scale(8)), 9).stream()
                        .filter(e -> e.getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize().dot(p.getLookAngle()) > 0.5)
                        .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(p))).toList();
                float yaw = p.getYRot(), pitch = p.getXRot();
                Vec3 muzzle = p.getEyePosition().add(p.getLookAngle().scale(0.9)), end = p.getEyePosition().add(p.getLookAngle().scale(12));
                if (!seen.isEmpty()) {
                    end = seen.get(n % seen.size()).getBoundingBox().getCenter();
                    Vec3 d = end.subtract(p.getEyePosition());
                    yaw = (float) (Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG);
                    pitch = (float) (-Mth.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)) * Mth.RAD_TO_DEG);
                }
                ServerLevel lv = p.serverLevel();
                // lock-on mark, then a tracer from the muzzle to it
                CastFx.gather(lv, end, CastFx.GOLD, 1.1f, 6);
                CastFx.streak(lv, muzzle, end, CastFx.GOLD, 0.035f, 5);
                CastFx.shards(lv, muzzle, CastFx.EMBER, 0.6f, 4);
                CastFx.shards(lv, end, CastFx.GOLD, 0.8f, 6);
                CastFx.sound(lv, muzzle, io.github.soundgoodizerfan.feedback.registry.FSounds.CAST_LOCK.get(), 0.7f, 1.0f + 0.12f * n);
                GunShots.triggerHeard(p.serverLevel(), p, held, yaw, pitch, System.currentTimeMillis(), false, InteractionHand.MAIN_HAND);
            });
        }
        return true;
    }
}
