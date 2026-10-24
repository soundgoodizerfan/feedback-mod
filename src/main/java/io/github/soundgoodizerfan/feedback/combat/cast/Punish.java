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

import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Punish: a blow that only lands on someone already knocked off their swing. */
final class Punish {

    private Punish() {
    }

    /** Punish: a heavy blow, more than double the weapon's damage, on the nearest thing in reach that is staggered; nothing else will take it. */
    static boolean act(ServerPlayer p) {
        if (p.getMainHandItem().getItem() instanceof GunItem)
            return false;
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        LivingEntity t = null;
        double best = 16;
        for (LivingEntity e : near(p, eye.add(look.scale(1.8)), 3)) {
            double d = e.distanceToSqr(p);
            if (CombatState.of(e).phase() == CombatPhase.STAGGER && d < best && e.getBoundingBox().getCenter().subtract(eye).normalize().dot(look) > 0.3) {
                best = d;
                t = e;
            }
        }
        if (t == null)
            return false;
        float damage = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        t.invulnerableTime = 0;
        t.hurt(p.damageSources().playerAttack(p), damage * 2.2f);
        stagger(p, t, damage * 2f * 10f);
        p.swing(InteractionHand.MAIN_HAND, true);
        ServerLevel lv = p.serverLevel();
        Vec3 c = t.getBoundingBox().getCenter();
        lv.sendParticles(ParticleTypes.CRIT, c.x, c.y, c.z, 24, 0.3, 0.3, 0.3, 0.5);
        lv.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        CastFx.shockwave(p, t.position().add(0, 0.1, 0), 2.5, 4, CastFx.dust(CastFx.CRIMSON, 1.3f), 0);
        CastFx.sound(lv, c, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 0.6f);
        CastFx.sound(lv, c, SoundEvents.ANVIL_LAND, 0.7f, 0.9f);
        return true;
    }
}
