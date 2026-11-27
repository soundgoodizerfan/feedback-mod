// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.core.unit.Work;

import io.github.soundgoodizerfan.feedback.combat.gear.CombatGear;
import io.github.soundgoodizerfan.feedback.combat.hit.BareMode;
import io.github.soundgoodizerfan.feedback.combat.hit.FistTuning;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.state.CombatTicker;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.later;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Uppercut: a short crouch, then a fist driven up through the chin that throws a body high into the air. */
final class Uppercut {

    /** Ticks of the whole clip and of the windup before the fist lands; Better Combat's uppercut is drawn on the off-hand slot for a fist. */
    private static final float CLIP = 16f;
    private static final int WINDUP = 6;

    private Uppercut() {
    }

    /** Uppercut: after a quick windup, one punch for seven damage that launches a body about seven blocks up. A miss leaves you staggered for a moment. */
    static boolean act(ServerPlayer p) {
        if (!BareMode.mainHand(p).isEmpty())
            return false;
        CastFx.clip(p, "one_handed_uppercut_right", CastFx.OFF, CLIP, WINDUP);
        boolean crouched = p.isShiftKeyDown();
        later(p, WINDUP, () -> {
            if (!p.isAlive())
                return;
            ServerLevel lv = p.serverLevel();
            Vec3 hand = p.getEyePosition().add(p.getLookAngle().scale(0.8)).add(0, -0.3, 0);
            LivingEntity t = ahead(p, 3.2, 0.5);
            if (t == null) {
                CombatTicker.stagger(p, 15, ParryTuning.STAGGER_SLOW);
                CastFx.streak(lv, hand, hand.add(0, 1.2, 0), CastFx.STEEL, 0.03f, 5);
                CastFx.sound(lv, hand, FSounds.CAST_WHOOSH.get(), 0.8f, 1.1f);
                return;
            }
            t.invulnerableTime = 0;
            float dmg = 7f + CombatGear.of(p).fist();
            t.hurt(p.damageSources().playerAttack(p), dmg);
            t.setDeltaMovement(t.getDeltaMovement().x, FistTuning.LAUNCH, t.getDeltaMovement().z);
            t.hurtMarked = true;
            if (crouched && io.github.soundgoodizerfan.feedback.combat.skill.Skills.unlocked(p, "rising_uppercut")) {
                p.setDeltaMovement(p.getDeltaMovement().x, FistTuning.LAUNCH, p.getDeltaMovement().z);
                p.hurtMarked = true;
            }
            stagger(p, t, new Work(dmg * ParryTuning.WORK_PER_DAMAGE));
            Vec3 c = t.getBoundingBox().getCenter();
            CastFx.streak(lv, c.add(0, -0.6, 0), c.add(0, 2.2, 0), 0xFFFFFF, 0.05f, 6);
            CastFx.shards(lv, c, CastFx.GOLD, 1.3f, 7);
            CastFx.sound(lv, c, FSounds.CAST_PUNCH.get(), 1f, 0.8f);
            CastFx.sound(lv, c, FSounds.CAST_WHOOSH.get(), 0.7f, 1.3f);
        });
        return true;
    }
}
