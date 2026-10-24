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
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
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
        float joules = io.github.soundgoodizerfan.feedback.weapon.GunFu.lastImpulse(p)
                * io.github.soundgoodizerfan.feedback.weapon.GunFuTuning.MUZZLE_JOULES_PER_IMPULSE
                * (io.github.soundgoodizerfan.feedback.combat.skill.Skills.unlocked(p, "concussive") ? 1.5f : 1f);
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        for (LivingEntity e : near(p, eye.add(look.scale(3)), 4)) {
            if (e.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) < 0.6)
                continue;
            stagger(p, e, joules);
            e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.BLINDNESS,
                    io.github.soundgoodizerfan.feedback.combat.skill.Skills.unlocked(p, "dazzle") ? 120 : 50));
        }
        ServerLevel lv = p.serverLevel();
        Vec3 muzzle = eye.add(look.scale(0.9));
        lv.sendParticles(ParticleTypes.FLASH, eye.x + look.x, eye.y + look.y, eye.z + look.z, 1, 0, 0, 0, 0);
        // the blast: a cone of fire and smoke thrown down the line of the shot, and a boom at its edge
        for (int i = 0; i < 28; i++) {
            Vec3 d = look.add((lv.random.nextDouble() - 0.5) * 0.9, (lv.random.nextDouble() - 0.5) * 0.9, (lv.random.nextDouble() - 0.5) * 0.9).normalize();
            lv.sendParticles(i % 3 == 0 ? ParticleTypes.SMOKE : ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 0, d.x, d.y, d.z, 0.5 + lv.random.nextDouble() * 0.5);
        }
        Vec3 edge = eye.add(look.scale(3));
        lv.sendParticles(ParticleTypes.SONIC_BOOM, edge.x, edge.y, edge.z, 1, 0, 0, 0, 0);
        CastFx.line(lv, CastFx.fade(0xFFFFFF, CastFx.EMBER, 1.2f), muzzle, eye.add(look.scale(6)), 0.4);
        CastFx.sound(lv, muzzle, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1f, 0.7f);
        CastFx.sound(lv, muzzle, SoundEvents.WARDEN_SONIC_BOOM, 0.5f, 1.8f);
        return true;
    }
}
