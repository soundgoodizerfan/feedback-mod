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

import io.github.soundgoodizerfan.feedback.combat.grab.Grabbing;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunFu;
import io.github.soundgoodizerfan.feedback.weapon.GunFuTuning;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Point Blank: the muzzle against a body that cannot move away. */
final class PointBlank {

    private PointBlank() {
    }

    /** Point blank: fire into a body you hold or one already staggered within reach; the whole of the round's impulse is stagger and, over ten joules to the point, damage. */
    static boolean act(ServerPlayer p) {
        ItemStack gun = p.getMainHandItem();
        if (!(gun.getItem() instanceof GunItem) || GunAmmo.count(gun) < 1)
            return false;
        LivingEntity t = Grabbing.held(p);
        if (t == null) {
            double best = 9;
            for (LivingEntity e : near(p, p.position(), 3)) {
                double d = e.distanceToSqr(p);
                if (CombatState.of(e).phase() == CombatPhase.STAGGER && d < best) {
                    best = d;
                    t = e;
                }
            }
        }
        if (t == null)
            return false;
        Vec3 eye = p.getEyePosition(), c = t.getBoundingBox().getCenter(), d = c.subtract(eye);
        float yaw = (float) (Mth.atan2(-d.x, d.z) * Mth.RAD_TO_DEG), pitch = (float) -(Mth.atan2(d.y, Math.hypot(d.x, d.z)) * Mth.RAD_TO_DEG);
        ServerLevel lv = p.serverLevel();
        if (!GunShots.trigger(lv, p, gun, yaw, pitch, System.currentTimeMillis(), false, InteractionHand.MAIN_HAND))
            return false;
        float joules = GunFu.lastImpulse(p) * GunFuTuning.MUZZLE_JOULES_PER_IMPULSE;
        t.invulnerableTime = 0;
        t.hurt(p.damageSources().playerAttack(p), Math.min(20f, joules / ParryTuning.JOULES_PER_DAMAGE));
        stagger(p, t, joules);
        lv.sendParticles(ParticleTypes.FLASH, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        lv.sendParticles(ParticleTypes.SMOKE, c.x, c.y, c.z, 14, 0.25, 0.25, 0.25, 0.05);
        CastFx.spray(lv, CastFx.dust(CastFx.CRIMSON, 1.1f), c, 18, 0.35);
        CastFx.sound(lv, c, SoundEvents.GENERIC_EXPLODE.value(), 0.5f, 1.8f);
        return true;
    }
}
