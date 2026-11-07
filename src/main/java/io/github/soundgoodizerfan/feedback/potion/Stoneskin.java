// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FEffects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Stoneskin: incoming damage is cut to a fraction (never nothing). Knockback resistance and the slowing (the price of a heavy body) are attribute
 * modifiers on the effect itself ({@link FEffects#STONESKIN}), so they scale per level and vanish with it.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Stoneskin {

    private Stoneskin() {}

    @SubscribeEvent
    public static void onDamage(LivingIncomingDamageEvent event) {
        LivingEntity who = event.getEntity();
        MobEffectInstance e = who.getEffect(FEffects.STONESKIN);
        if (e == null || who.level().isClientSide)
            return;
        float f = Math.max(PotionTuning.STONESKIN_DAMAGE_FLOOR, PotionTuning.STONESKIN_DAMAGE - PotionTuning.STONESKIN_DAMAGE_PER_LEVEL * e.getAmplifier());
        if (who.hasEffect(MobEffects.DAMAGE_RESISTANCE))
            f = Math.max(f, PotionTuning.STONESKIN_WITH_RESISTANCE);
        event.setAmount(event.getAmount() * f);
    }
}
