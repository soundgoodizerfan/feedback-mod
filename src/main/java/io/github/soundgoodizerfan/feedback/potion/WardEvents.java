// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Where wards cut a hazard that has no code of the mod's own to hook. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class WardEvents {

    private WardEvents() {}

    /** Vanilla poison ticks as sourceless magic damage on a poisoned body; every poison source (fumes, food, stings) lands here. */
    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        var who = event.getEntity();
        if (event.getSource().is(DamageTypes.MAGIC) && event.getSource().getEntity() == null && who.hasEffect(MobEffects.POISON))
            event.setAmount(event.getAmount() * Ward.passes(who, Ward.Hazard.POISON));
    }
}
