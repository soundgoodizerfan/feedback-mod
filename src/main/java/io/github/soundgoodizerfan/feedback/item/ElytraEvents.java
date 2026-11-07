// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** A firework rocket used while gliding does nothing (the boost is refused, the rocket kept); crossbows and displays are untouched. */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class ElytraEvents {

    private ElytraEvents() {
    }

    @SubscribeEvent
    public static void onUse(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().isFallFlying() && event.getItemStack().is(Items.FIREWORK_ROCKET)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }
}
