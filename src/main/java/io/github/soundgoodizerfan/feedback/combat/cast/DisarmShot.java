// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunFu;
import io.github.soundgoodizerfan.feedback.weapon.GunFuTuning;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Disarm Shot: a round placed on what a man is holding. */
final class DisarmShot {

    private DisarmShot() {
    }

    /** Disarm shot: a round aimed at the weapon hand of whoever you face within twenty blocks; if it leaves the muzzle, what they held falls at their feet and they flinch by the round's impulse. [OPEN] disarms on fire, not on a confirmed hit. */
    static boolean act(ServerPlayer p) {
        ItemStack gun = p.getMainHandItem();
        if (!(gun.getItem() instanceof GunItem) || GunAmmo.count(gun) < 1)
            return false;
        LivingEntity t = ahead(p, 20, 0.95);
        if (t == null || t.getMainHandItem().isEmpty() || !p.hasLineOfSight(t))
            return false;
        Vec3 eye = p.getEyePosition(), hand = t.position().add(0, t.getBbHeight() * 0.65, 0), d = hand.subtract(eye);
        float yaw = (float) (Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG), pitch = (float) -(Mth.atan2(d.y, Math.hypot(d.x, d.z)) * Mth.RAD_TO_DEG);
        if (!GunShots.trigger(p.serverLevel(), p, gun, yaw, pitch, System.currentTimeMillis(), true, InteractionHand.MAIN_HAND))
            return false;
        ItemStack dropped = t.getMainHandItem().copy();
        t.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        t.spawnAtLocation(dropped);
        Disorder.note(p, t, Disorder.Kind.STRIPPED);
        stagger(p, t, GunFu.lastImpulse(p) * GunFuTuning.MUZZLE_JOULES_PER_IMPULSE);
        p.serverLevel().sendParticles(ParticleTypes.CRIT, hand.x, hand.y, hand.z, 14, 0.2, 0.2, 0.2, 0.4);
        p.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y, hand.z, 10, 0.2, 0.2, 0.2, 0.5);
        CastFx.sound(p.serverLevel(), hand, SoundEvents.ANVIL_PLACE, 0.8f, 1.8f);
        return true;
    }
}
