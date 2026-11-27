// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.core.unit.Work;

import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Muzzle Flash: a shot fired as a blast. */
final class MuzzleFlash {

    private MuzzleFlash() {
    }

    /** Muzzle flash: a shot fired as a blast. The round's baked impulse is the stagger of everything in the cone in front, and the flash blinds it. */
    static boolean act(ServerPlayer p) {
        ItemStack gun = p.getMainHandItem();
        if (!(gun.getItem() instanceof GunItem) || GunAmmo.count(gun) < 1
                || !GunShots.triggerHeard(p.serverLevel(), p, gun, p.getYRot(), p.getXRot(), System.currentTimeMillis(), false, InteractionHand.MAIN_HAND))
            return false;
        float work = io.github.soundgoodizerfan.feedback.weapon.GunFu.lastImpulse(p)
                * io.github.soundgoodizerfan.feedback.weapon.GunFuTuning.MUZZLE_JOULES_PER_IMPULSE
                * (io.github.soundgoodizerfan.feedback.combat.skill.Skills.unlocked(p, "concussive") ? 1.5f : 1f);
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        for (LivingEntity e : near(p, eye.add(look.scale(3)), 4)) {
            if (e.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.6)
                continue;
            stagger(p, e, new Work(work));
            // Concussive: the blast drives a dose of the pack's potion into every body it staggers.
            if (io.github.soundgoodizerfan.feedback.combat.skill.Skills.unlocked(p, "concussive"))
                Vials.dose(p, e, Vials.draw(p, io.github.soundgoodizerfan.feedback.potion.PotionTuning.SPRAY_DOSE_MB));
            Disorder.note(p, e, Disorder.Kind.BLINDED);
            e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS,
                    io.github.soundgoodizerfan.feedback.combat.skill.Skills.unlocked(p, "dazzle") ? 120 : 50));
        }
        ServerLevel lv = p.serverLevel();
        Vec3 muzzle = eye.add(look.scale(0.9));
        // the blast: a white flash of shards at the muzzle, tracers down the line of the shot, and a ring at its edge
        CastFx.shards(lv, muzzle, 0xFFFFFF, 1.6f, 6);
        Vec3 edge = eye.add(look.scale(3));
        // Dazzle: the flash lights a bottle dashed ahead of it, and the mist hangs where the blast ends.
        if (io.github.soundgoodizerfan.feedback.combat.skill.Skills.unlocked(p, "dazzle")) {
            var drawn = Vials.draw(p, io.github.soundgoodizerfan.feedback.potion.PotionTuning.SPRAY_DOSE_MB * 4);
            if (!drawn.isEmpty()) {
                io.github.soundgoodizerfan.feedback.potion.PotionSpray.mist(lv, edge, drawn, drawn.getAmount() * 2);
                Vials.splash(lv, edge, drawn);
            }
        }
        CastFx.pulse(lv, edge, look, CastFx.EMBER, 2.5f, 9);
        CastFx.streak(lv, muzzle, eye.add(look.scale(6)), 0xFFFFFF, 0.05f, 5);
        CastFx.streak(lv, muzzle, eye.add(look.scale(5)).add(0, 0.4, 0), CastFx.EMBER, 0.03f, 5);
        CastFx.streak(lv, muzzle, eye.add(look.scale(5)).add(0, -0.4, 0), CastFx.EMBER, 0.03f, 5);
        CastFx.sound(lv, muzzle, FSounds.CAST_SLAM.get(), 1f, 1.3f);
        CastFx.sound(lv, muzzle, FSounds.CAST_SURGE.get(), 0.5f, 1.4f);
        return true;
    }
}
