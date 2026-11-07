// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularFigures;
import io.github.soundgoodizerfan.feedback.modular.ModularStacks;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

/** The wheel over an item whose tooltip has more readings than fit scrolls them. */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class ReadingScroll {

    private ReadingScroll() {
    }

    @SubscribeEvent
    public static void onScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!(event.getScreen() instanceof AbstractContainerScreen<?> screen))
            return;
        Slot slot = screen.getSlotUnderMouse();
        if (slot == null)
            return;
        ModularFigures f = ModularStacks.figures(slot.getItem());
        if (f == null)
            return;
        int size = ModularReadings.readings(slot.getItem(), f).size();
        if (size <= ModularReadings.VISIBLE)
            return;
        int next = ModularReadings.scroll - (int) Math.signum(event.getScrollDeltaY());
        ModularReadings.scroll = Math.max(0, Math.min(next, size - ModularReadings.VISIBLE));
        event.setCanceled(true);
    }
}
