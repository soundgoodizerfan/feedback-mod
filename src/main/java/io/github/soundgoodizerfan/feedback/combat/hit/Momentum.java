// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.hit;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.skill.SkillTree;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;
import io.github.soundgoodizerfan.feedback.combat.tether.BodyMass;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Momentum hits (Brute): a melee blow lands harder by the striker's speed times mass. The share is the
 * {@link SkillTree.Figure#MOMENTUM_HIT} figure, scaled by horizontal speed over {@link #SPRINT} and by body plus armour
 * mass over {@link #REFERENCE_MU}, so a charge in plate hits harder than a standing swing and a light body gains
 * little. Mass and speed only, never identity.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class Momentum {

    /** STAND-IN: blocks per tick of a sprint; Mu of an average body with light armour; caps on each factor. */
    static final double SPRINT = 0.28;
    static final float REFERENCE_MU = 90f, MAX_SPEED = 1.5f, MIN_MASS = 0.5f, MAX_MASS = 2f;

    private Momentum() {
    }

    @SubscribeEvent
    static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer p) || event.getSource().getDirectEntity() != p)
            return;
        float figure = Skills.modifier(p, SkillTree.Figure.MOMENTUM_HIT);
        if (figure <= 0f)
            return;
        double speed = Math.hypot(p.getX() - p.xo, p.getZ() - p.zo);
        float speedShare = (float) Math.min(MAX_SPEED, speed / SPRINT);
        float massShare = Math.max(MIN_MASS, Math.min(MAX_MASS, (BodyMass.of(p) + BodyMass.armour(p)) / REFERENCE_MU));
        event.setAmount(event.getAmount() * (1f + figure * speedShare * massShare));
    }
}
