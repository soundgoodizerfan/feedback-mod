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

import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.core.hazard.HotContact;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.weapon.GunCondition;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;

/** Overheat: the barrel's heat, thrown out as a cone of fire. */
final class Overheat {

    private Overheat() {
    }

    /** The degrees above the room a barrel must be to have anything to vent. */
    private static final float MIN_ABOVE_AMBIENT = 150f;

    /** Overheat: vent a hot barrel (150 Tu over the room or more) as a burning cone; the hotter, the longer and the longer it burns. The barrel loses two thirds of its heat and scalds the hand that holds it. */
    static boolean act(ServerPlayer p) {
        ItemStack gun = p.getMainHandItem();
        if (!(gun.getItem() instanceof GunItem))
            return false;
        ServerLevel lv = p.serverLevel();
        float ambient = AmbientTable.at(lv).value(), temp = GunCondition.temperature(gun, lv).value(), above = temp - ambient;
        if (above < MIN_ABOVE_AMBIENT)
            return false;
        double reach = Mth.clamp(3 + above / 150, 3, 8);
        float seconds = (4 + above / 200) * (Skills.unlocked(p, "scorch") ? 2f : 1f);
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        for (LivingEntity e : near(p, eye.add(look.scale(reach / 2)), reach / 2 + 1)) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            if (to.length() > reach || to.normalize().dot(look) < 0.7)
                continue;
            e.igniteForSeconds(seconds);
            e.invulnerableTime = 0;
            e.hurt(p.damageSources().inFire(), 3f);
        }
        ItemHeat.set(gun, new Tu(ambient + above / 3f), lv);
        HotContact.burn(p, new Tu(temp));
        Vec3 muzzle = eye.add(look.scale(0.9));
        for (int i = 0; i < 40; i++) {
            Vec3 d = look.add((lv.random.nextDouble() - 0.5) * 0.5, (lv.random.nextDouble() - 0.5) * 0.5, (lv.random.nextDouble() - 0.5) * 0.5).normalize();
            lv.sendParticles(i % 4 == 0 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.FLAME, muzzle.x, muzzle.y, muzzle.z, 0, d.x, d.y, d.z, 0.25 + reach * 0.06 * lv.random.nextDouble());
        }
        CastFx.sound(lv, muzzle, SoundEvents.BLAZE_SHOOT, 1f, 0.7f);
        CastFx.sound(lv, muzzle, SoundEvents.LAVA_EXTINGUISH, 0.8f, 0.9f);
        return true;
    }
}
