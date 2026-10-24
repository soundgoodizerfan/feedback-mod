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
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
                CastFx.ring(lv, CastFx.dust(CastFx.GOLD, 1.0f), end, 0.7, 12, 0);
                CastFx.line(lv, CastFx.fade(CastFx.GOLD, CastFx.EMBER, 0.9f), muzzle, end, 0.5);
                lv.sendParticles(ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 4, 0.05, 0.05, 0.05, 0.04);
                lv.sendParticles(ParticleTypes.CRIT, end.x, end.y, end.z, 6, 0.2, 0.2, 0.2, 0.2);
                CastFx.sound(lv, muzzle, SoundEvents.FIREWORK_ROCKET_BLAST, 0.5f, 1.2f + 0.15f * n);
                GunShots.triggerHeard(p.serverLevel(), p, held, yaw, pitch, System.currentTimeMillis(), false, InteractionHand.MAIN_HAND);
            });
        }
        return true;
    }
}
