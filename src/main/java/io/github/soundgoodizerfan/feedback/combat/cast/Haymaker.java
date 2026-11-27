// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.core.unit.Work;

import io.github.soundgoodizerfan.feedback.combat.gear.CombatGear;
import io.github.soundgoodizerfan.feedback.combat.hit.BareMode;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.later;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Haymaker: a slow wound-up punch that sends a body flying, and leaves you open if it finds nothing. */
final class Haymaker {

    private Haymaker() {
    }

    /** Haymaker: wind up for half a second, then throw one punch for nine damage (a worn plate adds its fist damage) that shoves a body back by your mass over its. A miss leaves you staggered. */
    static boolean act(ServerPlayer p) {
        if (!BareMode.mainHand(p).isEmpty())
            return false;
        Vec3 hand = p.getEyePosition().add(p.getLookAngle().scale(0.6)).add(0, -0.4, 0);
        CastFx.gather(p.serverLevel(), hand, CastFx.EMBER, 1.4f, 10);
        CastFx.sound(p.serverLevel(), hand, FSounds.CAST_SURGE.get(), 0.6f, 0.8f);
        CastFx.clip(p, "one_handed_swipe_horizontal_right", CastFx.MAIN, 20, 10);
        later(p, 10, () -> {
            if (!p.isAlive())
                return;
            p.swing(InteractionHand.MAIN_HAND, true);
            LivingEntity t = ahead(p, 3.4, 0.45);
            ServerLevel lv = p.serverLevel();
            if (t == null) {
                CombatTicker.stagger(p, 25, ParryTuning.STAGGER_SLOW); // the weight of a whiff
                CastFx.streak(lv, hand, hand.add(p.getLookAngle().scale(2.5)), 0xFFFFFF, 0.03f, 5);
                CastFx.sound(lv, hand, FSounds.CAST_WHOOSH.get(), 0.8f, 0.7f);
                return;
            }
            float mine = BodyMass.of(p) + BodyMass.armour(p), theirs = Math.max(1f, BodyMass.of(t) + BodyMass.armour(t));
            Vec3 look = p.getLookAngle().multiply(1, 0, 1).normalize();
            double shove = Mth.clamp(mine / theirs, 0.2, 2.0) * 0.9;
            t.invulnerableTime = 0;
            float dmg = 9f + CombatGear.of(p).fist();
            t.hurt(p.damageSources().playerAttack(p), dmg);
            t.push(look.x * shove, 0.3, look.z * shove);
            t.hurtMarked = true;
            stagger(p, t, new Work(dmg * ParryTuning.WORK_PER_DAMAGE));
            Vec3 c = t.getBoundingBox().getCenter();
            CastFx.streak(lv, hand, c, CastFx.EMBER, 0.04f, 4);
            CastFx.shards(lv, c, 0xFFFFFF, 2f, 9);
            CastFx.pulse(lv, t.position().add(0, 0.1, 0), Vec3.ZERO, CastFx.EMBER, 2.4f, 9);
            CastFx.sound(lv, c, FSounds.CAST_PUNCH.get(), 1f, 0.8f);
            CastFx.sound(lv, c, FSounds.CAST_SLAM.get(), 0.7f, 1.1f);
        });
        return true;
    }
}
