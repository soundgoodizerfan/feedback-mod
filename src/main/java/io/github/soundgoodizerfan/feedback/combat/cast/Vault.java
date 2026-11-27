// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.core.unit.Work;

import io.github.soundgoodizerfan.feedback.core.FTuning;

import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Vault: spring off a target's shoulders and land behind it. */
final class Vault {

    private Vault() {
    }

    /** Vault: leap over the nearest thing in front and land behind it; it is staggered by your mass pushed off it. */
    static boolean act(ServerPlayer p) {
        LivingEntity t = ahead(p, 4, 0.5);
        if (t == null || p.isPassenger())
            return false;
        Vec3 over = t.position().subtract(p.position()).multiply(1, 0, 1);
        if (over.lengthSqr() < 1e-6)
            return false;
        over = over.normalize();
        double reach = t.distanceTo(p) + 1.8;
        p.setDeltaMovement(over.x * reach / 8.5, 0.62, over.z * reach / 8.5); // ~16 ticks aloft, air drag 0.91 a tick
        p.hurtMarked = true;
        p.fallDistance = 0f;
        stagger(p, t, new Work((BodyMass.of(p) + BodyMass.armour(p)) * FTuning.GRAVITY_BLOCKS_PER_S2 * 1.2f));
        ServerLevel lv = p.serverLevel();
        Vec3 shoulders = t.position().add(0, t.getBbHeight(), 0);
        CastFx.pulse(lv, shoulders, Vec3.ZERO, CastFx.GOLD, 1f, 6);
        CastFx.streak(lv, p.position().add(0, 0.5, 0), shoulders, CastFx.GOLD, 0.04f, 5);
        CastFx.sound(lv, shoulders, FSounds.CAST_WHOOSH.get(), 0.8f, 1.3f);
        CombatCues.everyone(p, FSounds.ROLL_WHOOSH.get(), FeelTuning.ROLL_VOLUME, 1.1f);
        return true;
    }
}
