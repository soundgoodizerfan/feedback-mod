// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.state.CombatPhase;
import io.github.soundgoodizerfan.feedback.combat.state.CombatState;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.near;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Stomp: the whole of your weight on something already knocked off its feet. */
final class Stomp {

    private Stomp() {
    }

    /** Stomp: on the nearest staggered thing within reach, your mass comes down as damage and a fresh stagger. Nothing that is standing steady will take it. */
    static boolean act(ServerPlayer p) {
        LivingEntity t = null;
        double best = 9;
        for (LivingEntity e : near(p, p.position(), 3)) {
            double d = e.distanceToSqr(p);
            if (CombatState.of(e).phase() == CombatPhase.STAGGER && d < best) {
                best = d;
                t = e;
            }
        }
        if (t == null)
            return false;
        float mass = BodyMass.of(p) + BodyMass.armour(p);
        float damage = (3f + mass / 10f) * (Skills.unlocked(p, "crush") ? 1.5f : 1f);
        t.invulnerableTime = 0;
        t.hurt(p.damageSources().playerAttack(p), damage);
        stagger(p, t, mass * 9.8f * 1.5f);
        p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        ServerLevel lv = p.serverLevel();
        Vec3 at = t.position().add(0, 0.2, 0);
        CastFx.smash(lv, at, 400, false);
        return true;
    }
}
