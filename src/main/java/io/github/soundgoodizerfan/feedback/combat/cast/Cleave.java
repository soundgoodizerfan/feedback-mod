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

import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
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
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.later;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Cleave: an overhead blow whose weight travels on through the ground. */
final class Cleave {

    private Cleave() {
    }

    /** Cleave: raise the weapon, bring it down; the target takes the blow and a crack runs seven blocks on, staggering by the weapon's damage and fading with distance. Needs a heavy weapon (damage 5 or more). */
    static boolean act(ServerPlayer p) {
        float damage = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        if (p.getMainHandItem().getItem() instanceof GunItem || damage < 5f)
            return false;
        Vec3 hand = p.getEyePosition().add(p.getLookAngle().scale(0.6)).add(0, 0.6, 0);
        CastFx.converge(p, hand, 1.2, 6, CastFx.dust(CastFx.STEEL, 1.2f));
        later(p, 6, () -> {
            if (!p.isAlive())
                return;
            p.swing(InteractionHand.MAIN_HAND, true);
            ServerLevel lv = p.serverLevel();
            Vec3 look = p.getLookAngle().multiply(1, 0, 1);
            if (look.lengthSqr() < 1e-6)
                return;
            Vec3 d = look.normalize(), start = p.position().add(d.scale(1.5));
            LivingEntity t = ahead(p, 3.4, 0.5);
            if (t != null) {
                t.invulnerableTime = 0;
                t.hurt(p.damageSources().playerAttack(p), damage * 1.3f);
            }
            for (LivingEntity e : near(p, start.add(d.scale(3.5)), 4.2)) {
                Vec3 off = e.position().subtract(start);
                double along = off.dot(d), side = off.subtract(d.scale(along)).multiply(1, 0, 1).length();
                if (along < 0 || along > 7 || side > 1.4 || e == t)
                    continue;
                stagger(p, e, damage * ParryTuning.JOULES_PER_DAMAGE * (float) (1 - along / 9));
            }
            CastFx.line(lv, CastFx.dust(CastFx.EMBER, 1.2f), start.add(0, 0.1, 0), start.add(d.scale(7)).add(0, 0.1, 0), 0.25);
            for (int i = 1; i <= 7; i++)
                CastFx.debris(lv, start.add(d.scale(i)).add(0, 0.2, 0), 10, 0.3);
            lv.sendParticles(ParticleTypes.EXPLOSION, start.x, start.y + 0.2, start.z, 1, 0, 0, 0, 0);
            CastFx.sound(lv, start, SoundEvents.ANVIL_LAND, 1f, 0.5f);
            CastFx.sound(lv, start, SoundEvents.GENERIC_EXPLODE.value(), 0.5f, 1.4f);
        });
        return true;
    }
}
