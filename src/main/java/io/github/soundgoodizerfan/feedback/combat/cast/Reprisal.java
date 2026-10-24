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
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Reprisal: every parry is answered with a blow at the one who struck. */
final class Reprisal {

    private Reprisal() {
    }

    /** The game tick until which Reprisal is live. */
    private static final java.util.Map<java.util.UUID, Long> REPRISAL = new java.util.HashMap<>();

    /** A parry landed on {@code p}: answer {@code source}'s attacker if Reprisal is up. */
    static void answer(ServerPlayer p, net.minecraft.world.damagesource.DamageSource source) {
        long now = p.level().getGameTime();
        if (source.getEntity() instanceof LivingEntity foe && foe != p && REPRISAL.getOrDefault(p.getUUID(), 0L) >= now && foe.distanceToSqr(p) < 49) {
            float damage = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
            foe.invulnerableTime = 0;
            foe.hurt(p.damageSources().playerAttack(p), damage);
            stagger(p, foe, damage * ParryTuning.JOULES_PER_DAMAGE);
            p.swing(InteractionHand.MAIN_HAND, true);
            // the answer: a streak from you to the one who struck, and a cut where it lands
            ServerLevel lv = p.serverLevel();
            Vec3 from = p.getEyePosition().add(0, -0.4, 0), to = foe.getBoundingBox().getCenter();
            CastFx.line(lv, CastFx.fade(CastFx.GOLD, 0xFFFFFF, 1.2f), from, to, 0.25);
            lv.sendParticles(ParticleTypes.SWEEP_ATTACK, to.x, to.y, to.z, 1, 0, 0, 0, 0);
            CastFx.spray(lv, ParticleTypes.ELECTRIC_SPARK, to, 20, 0.5);
            CastFx.sound(lv, to, SoundEvents.PLAYER_ATTACK_CRIT, 1f, 1.2f);
            CastFx.sound(lv, to, SoundEvents.TRIDENT_RETURN, 0.8f, 1.6f);
        }
    }

    /** Reprisal: for five seconds, every parry you land is answered with a blow at the one who struck. */
    static boolean act(ServerPlayer p) {
        REPRISAL.put(p.getUUID(), p.level().getGameTime() + 100);
        Vec3 chest = p.position().add(0, 1, 0);
        CastFx.converge(p, chest, 1.6, 5, CastFx.dust(CastFx.GOLD, 1.1f));
        CastFx.ring(p.serverLevel(), ParticleTypes.CRIT, chest, 0.8, 14, 0.3);
        CastFx.sound(p.serverLevel(), chest, SoundEvents.TRIDENT_RETURN, 0.8f, 0.9f);
        CastFx.sound(p.serverLevel(), chest, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 1.2f);
        CombatCues.everyone(p, FSounds.FEEL_LIGHT.get(), FeelTuning.LIGHT_VOLUME, 0.5f);
        return true;
    }
}
