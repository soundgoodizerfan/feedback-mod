// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;

/** Called Shot: a round fired not at where the target is but at where it will be. */
final class CalledShot {

    private CalledShot() {
    }

    /** Called shot: the nearest thing under your crosshair within forty blocks, in line of sight; the round is aimed at where its motion puts it by the time the round arrives (about 2.5 blocks a tick). */
    static boolean act(ServerPlayer p) {
        ItemStack gun = p.getMainHandItem();
        // Called draw: with a bow or crossbow in hand the call is made on the creature, and the next arrow flies at where it will be.
        if (gun.getItem() instanceof net.minecraft.world.item.ProjectileWeaponItem && !(gun.getItem() instanceof GunItem)
                && io.github.soundgoodizerfan.feedback.combat.skill.Skills.unlocked(p, "called_draw")) {
            LivingEntity mark = ahead(p, 40, 0.97);
            if (mark == null || !p.hasLineOfSight(mark))
                return false;
            io.github.soundgoodizerfan.feedback.combat.skill.BowCraft.call(p, mark);
            CastFx.ring(p.serverLevel(), CastFx.dust(CastFx.GOLD, 1.0f), mark.getBoundingBox().getCenter(), 0.5, 12, 0);
            CastFx.sound(p.serverLevel(), p.getEyePosition(), SoundEvents.ARROW_HIT_PLAYER, 0.6f, 1.5f);
            return true;
        }
        if (!(gun.getItem() instanceof GunItem) || GunAmmo.count(gun) < 1)
            return false;
        LivingEntity t = ahead(p, 40, 0.97);
        if (t == null || !p.hasLineOfSight(t))
            return false;
        Vec3 eye = p.getEyePosition(), c = t.getBoundingBox().getCenter();
        double flight = c.distanceTo(eye) / 2.5;
        Vec3 lead = c.add(t.getDeltaMovement().multiply(1, t.onGround() ? 0 : 1, 1).scale(flight)), d = lead.subtract(eye);
        float yaw = (float) (Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG), pitch = (float) -(Mth.atan2(d.y, Math.hypot(d.x, d.z)) * Mth.RAD_TO_DEG);
        if (!GunShots.trigger(p.serverLevel(), p, gun, yaw, pitch, System.currentTimeMillis(), true))
            return false;
        CastFx.ring(p.serverLevel(), CastFx.dust(CastFx.GOLD, 1.0f), lead, 0.5, 12, 0);
        CastFx.line(p.serverLevel(), CastFx.dust(CastFx.GOLD, 0.6f), c, lead, 0.3);
        CastFx.sound(p.serverLevel(), eye, SoundEvents.ARROW_HIT_PLAYER, 0.6f, 1.5f);
        return true;
    }
}
