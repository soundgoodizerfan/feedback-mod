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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.later;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;

/** Snap Volley: three quick rounds, each at the next target in front. */
final class SnapVolley {

    private SnapVolley() {
    }

    private static boolean fireAt(ServerPlayer p, LivingEntity t) {
        ItemStack gun = p.getMainHandItem();
        if (!(gun.getItem() instanceof GunItem) || !t.isAlive())
            return false;
        Vec3 eye = p.getEyePosition(), d = t.getBoundingBox().getCenter().subtract(eye);
        float yaw = (float) (Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG), pitch = (float) -(Mth.atan2(d.y, Math.hypot(d.x, d.z)) * Mth.RAD_TO_DEG);
        return GunShots.trigger(p.serverLevel(), p, gun, yaw, pitch, System.currentTimeMillis(), false, InteractionHand.MAIN_HAND);
    }

    /** Snap volley: up to three rounds, four ticks apart, each at the next nearest living thing in the forward cone (the same one again if it is alone). The gun's own cycle may refuse a follow-up; each round spends ammo and adds heat as any shot. */
    static boolean act(ServerPlayer p) {
        ItemStack gun = p.getMainHandItem();
        if (!(gun.getItem() instanceof GunItem) || GunAmmo.count(gun) < 1)
            return false;
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        List<LivingEntity> targets = near(p, eye.add(look.scale(10)), 12).stream()
                .filter(e -> e.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) > 0.5 && p.hasLineOfSight(e))
                .sorted(Comparator.comparingDouble(e -> e.distanceToSqr(p))).toList();
        if (targets.isEmpty() || !fireAt(p, targets.get(0)))
            return false;
        for (int i = 1; i < 3; i++) {
            LivingEntity t = targets.get(i % targets.size());
            later(p, i * 4, () -> {
                if (p.isAlive())
                    fireAt(p, t);
            });
        }
        CastFx.sound(p.serverLevel(), eye, SoundEvents.CROSSBOW_QUICK_CHARGE_3.value(), 0.6f, 1.6f);
        return true;
    }
}
