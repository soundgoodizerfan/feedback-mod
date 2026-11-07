// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FEffects;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;

/**
 * Second wind and its payback. While it lasts, stamina refills faster and starts sooner ({@code combat/cast/Stamina} asks here); when it runs
 * out, the same stimulant is repaid as {@link FEffects#WINDED}, a stall in stamina regen as long as the wind was strong. No second meter: it only
 * bends the one stamina bar's regen.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class SecondWind {

    private SecondWind() {}

    /** Stamina regen multiplier: 0 while winded, raised per level of second wind, else 1. */
    public static float regenFactor(Player p) {
        if (p.hasEffect(FEffects.WINDED))
            return 0f;
        MobEffectInstance wind = p.getEffect(FEffects.SECOND_WIND);
        float assay = p.hasEffect(FEffects.ASSAY) ? PotionTuning.ASSAY_REGEN : 1f; // fine focus costs breadth
        return assay * (wind == null ? 1f : 1f + PotionTuning.SECOND_WIND_REGEN * (wind.getAmplifier() + 1));
    }

    /** Ticks after a spend before stamina refills: shorter under second wind. */
    public static int delay(Player p, int base) {
        return p.hasEffect(FEffects.SECOND_WIND) ? Math.round(base * PotionTuning.SECOND_WIND_DELAY) : base;
    }

    /** The wind runs out: the crash, longer for a stronger wind. */
    @SubscribeEvent
    public static void onExpired(MobEffectEvent.Expired event) {
        MobEffectInstance e = event.getEffectInstance();
        if (e == null || !e.is(FEffects.SECOND_WIND) || event.getEntity().level().isClientSide)
            return;
        event.getEntity().addEffect(new MobEffectInstance(FEffects.WINDED, PotionTuning.WINDED_TICKS * (e.getAmplifier() + 1)));
    }
}
