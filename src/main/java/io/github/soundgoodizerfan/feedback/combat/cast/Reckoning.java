// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.core.unit.Work;

import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.ahead;
import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Reckoning: the energy of the last parried blow, sent back through the weapon as one strike scaled by that blow only. */
final class Reckoning {

    private Reckoning() {
    }

    /** The last parried blow's damage, and when it was parried (a new parry replaces it; nothing adds up). */
    private static final java.util.Map<java.util.UUID, float[]> BANK = new java.util.HashMap<>();

    /** A parry landed on {@code p} for {@code amount}: it is the blow Reckoning will send back. */
    static void bank(ServerPlayer p, float amount) {
        if (!Skills.unlocked(p, "reckoning"))
            return;
        long now = p.level().getGameTime();
        float[] bank = BANK.computeIfAbsent(p.getUUID(), k -> new float[]{0, now});
        bank[0] = Math.min(CastTuning.RECKON_MAX, amount);
        bank[1] = now;
        // the blow is visible in the weapon: sparks drawn into the hand, the pitch climbing with its weight
        ServerLevel lv = p.serverLevel();
        Vec3 hand = p.getEyePosition().add(p.getLookAngle().scale(0.7)).add(0, -0.5, 0);
        CastFx.gather(lv, hand, CastFx.GOLD, 1.2f, 6);
        CastFx.sound(lv, hand, FSounds.CAST_SURGE.get(), 0.9f, Math.min(1.4f, 0.8f + bank[0] * 0.03f));
    }

    /** Reckoning: the last parried blow goes back through the weapon as one strike at what you face. */
    static boolean act(ServerPlayer p) {
        float[] bank = BANK.get(p.getUUID());
        if (bank == null || p.level().getGameTime() - (long) bank[1] > CastTuning.RECKON_TICKS || bank[0] <= 0)
            return false;
        LivingEntity t = ahead(p, 5, 0.6);
        if (t == null)
            return false;
        float damage = bank[0] * CastTuning.RECKON_SHARE;
        bank[0] = 0;
        t.invulnerableTime = 0;
        t.hurt(p.damageSources().playerAttack(p), damage);
        stagger(p, t, new Work(damage * ParryTuning.WORK_PER_DAMAGE * 2f));
        p.swing(InteractionHand.MAIN_HAND, true);
        CastFx.clip(p, "two_handed_slash_vertical_left", CastFx.TWO, 16, 4);
        ServerLevel lv = p.serverLevel();
        Vec3 from = p.getEyePosition().add(0, -0.4, 0), to = t.getBoundingBox().getCenter();
        CastFx.streak(lv, from, to, 0xFFFFFF, 0.05f, 5);
        CastFx.bolt(lv, from, to, CastFx.GOLD, 0.03f, 6);
        CastFx.slash(lv, to, to.subtract(from).normalize(), CastFx.GOLD, 2f, 8);
        CastFx.shards(lv, to, 0xFFFFFF, damage > 16 ? 3.2f : 2.4f, 10);
        CastFx.pulse(lv, t.position().add(0, 0.1, 0), Vec3.ZERO, CastFx.GOLD, 3.5f, 9);
        CastFx.column(lv, t.position(), CastFx.GOLD, 3f, 10);
        CastFx.sound(lv, to, FSounds.CAST_SLAM.get(), 1f, 0.9f);
        CastFx.sound(lv, to, FSounds.CAST_SURGE.get(), 0.8f, 1.3f);
        CombatCues.everyone(p, FSounds.FEEL_HEAVY.get(), FeelTuning.HEAVY_VOLUME, 0.8f);
        return true;
    }
}
