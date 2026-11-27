// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.core.unit.Work;

import io.github.soundgoodizerfan.feedback.core.FTuning;

import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
        if (!stagger(p, t, new Work(mine * FTuning.GRAVITY_BLOCKS_PER_S2)))
            return false;
        float v = Mth.clamp(mine / Math.max(1f, theirs) * SPEED_PER_RATIO, 2f, MAX_SPEED);
        Vec3 d = t.position().subtract(p.position()).multiply(1, 0, 1).normalize();
        t.setDeltaMovement(d.x * v / FTuning.TICKS_PER_SECOND, 0.25, d.z * v / FTuning.TICKS_PER_SECOND);
        t.hurtMarked = true;
        ServerLevel lv = p.serverLevel();
        CastFx.trail(lv, p, new Vec3(0, 1, 0), CastFx.STEEL, 0.15f, 6);
        CastFx.pulse(lv, t.position().add(0, 0.9, 0), d, CastFx.STEEL, 1.4f, 7);
        CastFx.sound(lv, t.position(), FSounds.CAST_PUNCH.get(), 1f, 0.7f);
        float work = 0.5f * theirs * v * v;
        Casts.later(p, BEAT, () -> {
            for (LivingEntity o : near(p, t.position().add(0, 0.5, 0), REACH)) {
                if (o == t)
                    continue;
                stagger(p, o, new Work(work));
                Disorder.note(p, o, Disorder.Kind.COLLIDED);
                o.hurt(p.damageSources().playerAttack(p), 1f + v / 4f);
                CastFx.shards(lv, o.position().add(0, 0.6, 0), CastFx.STEEL, 0.9f, 6);
                CastFx.sound(lv, o.position(), FSounds.CAST_PUNCH.get(), 1f, 0.7f);
            }
        });
        return true;
    }
}
