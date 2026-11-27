// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.core.unit.Work;

import io.github.soundgoodizerfan.feedback.combat.feel.CombatCues;
import io.github.soundgoodizerfan.feedback.combat.feel.FeelTuning;
import io.github.soundgoodizerfan.feedback.combat.parry.ParryTuning;
import io.github.soundgoodizerfan.feedback.registry.FSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import static io.github.soundgoodizerfan.feedback.combat.cast.Casts.stagger;

/** Reprisal, always on once owned (a rider on the parry, not a cast): every parry you land is answered at once with a blow at the one who struck. */
final class Reprisal {

    private Reprisal() {
    }

    /** A parry landed on {@code p}: answer {@code source}'s attacker if Reprisal is up. */
    static void answer(ServerPlayer p, net.minecraft.world.damagesource.DamageSource source) {
        if (source.getEntity() instanceof LivingEntity foe && foe != p && io.github.soundgoodizerfan.feedback.combat.skill.Skills.unlocked(p, "reprisal") && foe.distanceToSqr(p) < 49) {
            float damage = (float) p.getAttributeValue(Attributes.ATTACK_DAMAGE);
            foe.invulnerableTime = 0;
            foe.hurt(p.damageSources().playerAttack(p), damage);
            stagger(p, foe, new Work(damage * ParryTuning.WORK_PER_DAMAGE));
            p.swing(InteractionHand.MAIN_HAND, true);
            // the answer: a streak from you to the one who struck, and a cut where it lands
            ServerLevel lv = p.serverLevel();
            Vec3 from = p.getEyePosition().add(0, -0.4, 0), to = foe.getBoundingBox().getCenter();
            CastFx.streak(lv, from, to, CastFx.GOLD, 0.03f, 4);
            CastFx.slash(lv, to, to.subtract(from).normalize(), 0xFFFFFF, 1.2f, 7);
            CastFx.shards(lv, to, CastFx.GOLD, 0.9f, 6);
            CastFx.sound(lv, to, FSounds.CAST_SLASH.get(), 1f, 1.2f);
        }
    }
}
