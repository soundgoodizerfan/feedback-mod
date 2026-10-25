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

import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;

/** Hamstring: a low cut that slows hard. */
final class Hamstring {

    private Hamstring() {
    }

    /** Hamstring: a low cut at what you face, for most of the weapon's damage, that slows it hard so you pick the range. */
    static boolean act(ServerPlayer p) {
        if (p.getMainHandItem().getItem() instanceof GunItem)
            return false;
        LivingEntity t = ahead(p, 3.6, 0.6);
        if (t == null)
            return false;
        t.invulnerableTime = 0;
        t.hurt(p.damageSources().playerAttack(p), (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE) * 0.7f);
        t.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 80, 3));
        p.swing(InteractionHand.MAIN_HAND, true);
        CastFx.clip(p, "one_handed_slash_switch_blade_right", CastFx.MAIN, 12, 3);
        ServerLevel lv = p.serverLevel();
        lv.sendParticles(ParticleTypes.CRIT, t.getX(), t.getY() + 0.3, t.getZ(), 8, 0.2, 0.1, 0.2, 0.2);
        Vec3 side = p.getLookAngle().cross(new Vec3(0, 1, 0)).normalize().scale(0.9), low = t.position().add(0, 0.4, 0);
        CastFx.line(lv, CastFx.dust(CastFx.CRIMSON, 1.3f), low.add(side), low.subtract(side).add(0, -0.2, 0), 0.12);
        lv.sendParticles(ParticleTypes.SWEEP_ATTACK, low.x, low.y, low.z, 1, 0, 0, 0, 0);
        CastFx.spray(lv, CastFx.dust(CastFx.CRIMSON, 1.0f), low, 16, 0.25);
        CastFx.ring(lv, CastFx.dust(CastFx.ICE, 0.9f), t.position().add(0, 0.1, 0), 0.5, 10, 0);
        CastFx.sound(lv, low, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.8f);
        CastFx.sound(lv, low, SoundEvents.PLAYER_ATTACK_CRIT, 0.8f, 0.7f);
        return true;
    }
}
