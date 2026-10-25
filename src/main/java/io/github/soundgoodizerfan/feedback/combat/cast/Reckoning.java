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
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
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
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Reckoning: parried damage banked, let go as one blow. */
final class Reckoning {

    private Reckoning() {
    }

    /** The energy banked from parries, and when it last grew. */
    private static final java.util.Map<java.util.UUID, float[]> BANK = new java.util.HashMap<>();

    /** A parry landed on {@code p} for {@code amount}: bank it. */
    static void bank(ServerPlayer p, float amount) {
        if (!Skills.unlocked(p, "reckoning"))
            return;
        long now = p.level().getGameTime();
        float[] bank = BANK.computeIfAbsent(p.getUUID(), k -> new float[]{0, now});
        if (now - (long) bank[1] > 200)
            bank[0] = 0;
        bank[0] = Math.min(40f, bank[0] + amount);
        bank[1] = now;
        // the banked blow is visible: sparks drawn into the weapon hand, the pitch climbing as it fills
        ServerLevel lv = p.serverLevel();
        Vec3 hand = p.getEyePosition().add(p.getLookAngle().scale(0.7)).add(0, -0.5, 0);
        CastFx.converge(p, hand, 1.2, 4, CastFx.dust(CastFx.GOLD, 1.0f));
        lv.sendParticles(ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y, hand.z, 4 + (int) bank[0] / 4, 0.2, 0.2, 0.2, 0.3);
        CastFx.sound(lv, hand, SoundEvents.AMETHYST_BLOCK_CHIME, 1f, 0.8f + bank[0] * 0.03f);
    }

    /** Reckoning: the damage your parries took in the last ten seconds, banked, is let go as one blow at what you face. */
    static boolean act(ServerPlayer p) {
        float[] bank = BANK.get(p.getUUID());
        if (bank == null || p.level().getGameTime() - (long) bank[1] > 200 || bank[0] <= 0)
            return false;
        LivingEntity t = ahead(p, 5, 0.6);
        if (t == null)
            return false;
        float damage = bank[0] * 0.8f;
        bank[0] = 0;
        t.invulnerableTime = 0;
        t.hurt(p.damageSources().playerAttack(p), damage);
        stagger(p, t, damage * ParryTuning.JOULES_PER_DAMAGE * 2f);
        p.swing(InteractionHand.MAIN_HAND, true);
        CastFx.clip(p, "two_handed_slash_vertical_left", CastFx.TWO, 16, 4);
        ServerLevel lv = p.serverLevel();
        Vec3 from = p.getEyePosition().add(0, -0.4, 0), to = t.getBoundingBox().getCenter();
        CastFx.line(lv, CastFx.fade(0xFFFFFF, CastFx.GOLD, 1.8f), from, to, 0.2);
        CastFx.line(lv, ParticleTypes.ELECTRIC_SPARK, from, to, 0.8);
        lv.sendParticles(damage > 16 ? ParticleTypes.EXPLOSION_EMITTER : ParticleTypes.EXPLOSION, to.x, to.y, to.z, 1, 0, 0, 0, 0);
        lv.sendParticles(ParticleTypes.FLASH, to.x, to.y, to.z, 1, 0, 0, 0, 0);
        CastFx.spray(lv, ParticleTypes.ELECTRIC_SPARK, to, 40, 0.8);
        CastFx.shockwave(p, t.position().add(0, 0.1, 0), 3.5, 5, CastFx.dust(CastFx.GOLD, 1.5f), 0);
        CastFx.sound(lv, to, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.9f, 1.4f);
        CastFx.sound(lv, to, SoundEvents.ANVIL_LAND, 1f, 0.6f);
        CombatCues.everyone(p, FSounds.FEEL_HEAVY.get(), FeelTuning.HEAVY_VOLUME, 0.8f);
        return true;
    }
}
