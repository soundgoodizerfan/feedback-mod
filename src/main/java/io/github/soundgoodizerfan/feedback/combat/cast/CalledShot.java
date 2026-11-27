// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import net.minecraft.server.level.ServerPlayer;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
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
            CastFx.gather(p.serverLevel(), mark.getBoundingBox().getCenter(), CastFx.GOLD, 1f, 6);
            CastFx.sound(p.serverLevel(), p.getEyePosition(), FSounds.CAST_LOCK.get(), 0.7f, 1.2f);
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
        CastFx.gather(p.serverLevel(), lead, CastFx.GOLD, 1f, 6);
        CastFx.streak(p.serverLevel(), c, lead, CastFx.GOLD, 0.02f, 6);
        CastFx.sound(p.serverLevel(), eye, FSounds.CAST_LOCK.get(), 0.7f, 1.2f);
        return true;
    }
}
