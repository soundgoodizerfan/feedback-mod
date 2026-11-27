// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.core.unit.Work;

import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
            stagger(p, e, new Work(damage * ParryTuning.WORK_PER_DAMAGE));
        }
        // the blade comes round in eight beats: a spoke from the body to the reach, a bright tip, and a wake behind it
        Vec3 c = p.position().add(0, 1, 0);
        double a0 = Math.toRadians(p.getYRot());
        for (int i = 0; i < 8; i++) {
            int n = i;
            later(p, n, () -> {
                double a = a0 + n * Math.PI / 4;
                Vec3 dir = new Vec3(Math.cos(a), 0, Math.sin(a));
                CastFx.slash(level, c, dir, CastFx.STEEL, 2.2f, 5);
                if (n % 4 == 0)
                    CastFx.sound(level, c, FSounds.CAST_SLASH.get(), 1f, 1.1f - n * 0.05f);
            });
        }
        CastFx.pulse(level, c.add(0, -0.9, 0), Vec3.ZERO, 0xFFFFFF, 3f, 8);
        p.swing(InteractionHand.MAIN_HAND, true);
        CastFx.clip(p, "two_handed_spin", CastFx.TWO, 14, 4);
        CombatCues.everyone(p, FSounds.ROLL_WHOOSH.get(), FeelTuning.ROLL_VOLUME, 0.9f);
        return true;
    }
}
