// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Barge: a shoulder that sends one body into the next. */
final class Barge {

    private Barge() {
    }

    /** STAND-INS: the shoved body's speed in m/s per unit of your mass over its, the most it is sent, the beat it reaches its neighbours, and their reach. */
    private static final float SPEED_PER_RATIO = 6f, MAX_SPEED = 14f;
    private static final int BEAT = 5;
    private static final double REACH = 1.6;

    /** Barge: a shoulder into the body ahead; it is staggered by your mass and sent along your line at a speed that grows with your mass over its. Whatever it meets on the way is staggered by the shoved body's own mass at that speed, and a body too heavy to move is not shoved. */
    static boolean act(ServerPlayer p) {
        LivingEntity t = ahead(p, 2.8, 0.5);
        if (t == null)
            return false;
        float mine = BodyMass.of(p) + BodyMass.armour(p), theirs = BodyMass.of(t) + BodyMass.armour(t);
        if (!stagger(p, t, mine * 9.8f))
            return false;
        float v = Mth.clamp(mine / Math.max(1f, theirs) * SPEED_PER_RATIO, 2f, MAX_SPEED);
        Vec3 d = t.position().subtract(p.position()).multiply(1, 0, 1).normalize();
        t.setDeltaMovement(d.x * v / 20, 0.25, d.z * v / 20);
        t.hurtMarked = true;
        ServerLevel lv = p.serverLevel();
        CastFx.debris(lv, p.position(), 8, 0.4);
        lv.sendParticles(ParticleTypes.POOF, t.getX(), t.getY() + 0.5, t.getZ(), 8, 0.2, 0.3, 0.2, 0.05);
        CastFx.sound(lv, t.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1f, 0.7f);
        float joules = 0.5f * theirs * v * v;
        Casts.later(p, BEAT, () -> {
            for (LivingEntity o : near(p, t.position().add(0, 0.5, 0), REACH)) {
                if (o == t)
                    continue;
                stagger(p, o, joules);
                Disorder.note(p, o, Disorder.Kind.COLLIDED);
                o.hurt(p.damageSources().playerAttack(p), 1f + v / 4f);
                lv.sendParticles(ParticleTypes.CRIT, o.getX(), o.getY() + 0.6, o.getZ(), 10, 0.25, 0.3, 0.25, 0.3);
                CastFx.sound(lv, o.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.7f);
            }
        });
        return true;
    }
}
