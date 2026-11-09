// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.skill.Skills;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;

/** Lee side, always on once owned: crouched with a full block against you, knockback is halved (a tether's pull is halved in {@link Skills#tetherPullScale}). */
@EventBusSubscriber(modid = Feedback.MOD_ID)
final class LeeSide {

    private LeeSide() {
    }

    @SubscribeEvent
    public static void onKnockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer p && Skills.leeSide(p))
            event.setStrength(event.getStrength() * 0.5f);
    }
}
