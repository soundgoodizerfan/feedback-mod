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

import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.later;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Whirl: a full turn with the held weapon. */
final class Whirl {

    private Whirl() {
    }

    /** A full turn with the held weapon: everything within reach is struck and shoved out, by the weapon's own damage. */
    static boolean act(ServerPlayer p) {
        if (p.getMainHandItem().getItem() instanceof GunItem)
            return false;
        float damage = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
        ServerLevel level = p.serverLevel();
        for (LivingEntity e : near(p, p.position().add(0, 0.9, 0), 3.2)) {
            Vec3 away = e.position().subtract(p.position()).multiply(1, 0, 1);
            away = away.lengthSqr() < 1e-6 ? Vec3.ZERO : away.normalize();
            e.invulnerableTime = 0;
            e.hurt(p.damageSources().playerAttack(p), damage * 0.8f);
            e.push(away.x * 0.5, 0.2, away.z * 0.5);
            e.hurtMarked = true;
            stagger(p, e, damage * ParryTuning.JOULES_PER_DAMAGE);
        }
        // the blade comes round in eight beats: a spoke from the body to the reach, a bright tip, and a wake behind it
        Vec3 c = p.position().add(0, 1, 0);
        double a0 = Math.toRadians(p.getYRot());
        for (int i = 0; i < 8; i++) {
            int n = i;
            later(p, n, () -> {
                double a = a0 + n * Math.PI / 4;
                Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
                CastFx.line(level, CastFx.fade(0xFFFFFF, CastFx.STEEL, 1.3f), c.add(dir.scale(0.8)), c.add(dir.scale(3.2)), 0.3);
                Vec3 tip = c.add(dir.scale(3.0));
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, tip.x, tip.y, tip.z, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.CRIT, tip.x, tip.y, tip.z, 5, 0.15, 0.15, 0.15, 0.3);
                CastFx.ring(level, CastFx.dust(CastFx.STEEL, 1.0f), c.add(0, -0.9, 0), 0.8 + n * 0.3, 8, 0);
                if (n % 4 == 0)
                    CastFx.sound(level, c, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 1.1f - n * 0.05f);
            });
        }
        CastFx.ring(level, ParticleTypes.CLOUD, c.add(0, -0.9, 0), 0.6, 18, 0.3);
        p.swing(InteractionHand.MAIN_HAND, true);
        CastFx.clip(p, "two_handed_spin", CastFx.TWO, 14, 4);
        CombatCues.everyone(p, FSounds.ROLL_WHOOSH.get(), FeelTuning.ROLL_VOLUME, 0.9f);
        return true;
    }
}
