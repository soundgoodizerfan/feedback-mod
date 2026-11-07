// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FEffects;

import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;

/**
 * Adrenaline. While it lasts, cast techniques cost a fraction of their stamina ({@code combat/cast/Stamina.spend} asks here) and the body
 * does not heal: regeneration, a full belly and any other heal on any creature under it are refused. No second meter; it only bends the one stamina bar's cost.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class Adrenaline {

    private Adrenaline() {}

    /** The stamina a cast really costs: scaled down while adrenaline lasts. */
    public static float cost(Player p, float base) {
        return p.hasEffect(FEffects.ADRENALINE) ? base * PotionTuning.ADRENALINE_COST : base;
    }

    /** No healing of any kind on a player running on adrenaline (server side; the heal is cancelled before it lands). */
    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        if (!event.getEntity().level().isClientSide && event.getEntity().hasEffect(FEffects.ADRENALINE))
            event.setCanceled(true);
    }
}
