// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FEffects;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

/** The radiation ward's trade: the radical scavenger spends the body's repair, so every heal lands at a fraction while it lasts. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class RadiationWard {

    private RadiationWard() {}

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        var who = event.getEntity();
        if (!who.level().isClientSide && who.hasEffect(FEffects.RADIATION_WARD))
            event.setAmount(event.getAmount() * PotionTuning.RADIATION_WARD_HEAL);
    }
}
