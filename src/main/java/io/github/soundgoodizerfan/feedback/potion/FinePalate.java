// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FEffects;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

/**
 * Fine palate: alkaloid sharpens the tongue. Taste lacing ({@link Lacing#detectable}) is judged against thresholds scaled by
 * {@link #keenness}; the price is bitterness, a quarter of the saturation a meal gives is lost while it lasts. The saturation
 * trim is the least invasive cost: no new meter, no hazard, readable on the hunger bar.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class FinePalate {

    private FinePalate() {}

    /** Multiplier on the taste thresholds: 1 normally, lower with the effect (never zero, so the mystery stays). */
    public static float keenness(Player p) {
        return p.hasEffect(FEffects.FINE_PALATE) ? 1f - PotionTuning.PALATE_EASE : 1f;
    }

    @SubscribeEvent
    public static void onEaten(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof Player p && !p.level().isClientSide && p.hasEffect(FEffects.FINE_PALATE) && event.getItem().getFoodProperties(p) != null)
            p.getFoodData().setSaturation(p.getFoodData().getSaturationLevel() * (1f - PotionTuning.PALATE_SATURATION_LOSS));
    }
}
