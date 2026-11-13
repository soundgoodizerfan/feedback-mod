// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.tooltip;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** A misused instrument says so in words; the size of the offset is for the player to find against a reference. */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class DriftTooltip {

    private DriftTooltip() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        var drift = event.getItemStack().get(FDataComponents.DRIFT.get());
        if (drift != null && !drift.isEmpty() && event.getItemStack().getItem() instanceof Instrument)
            event.getToolTip().add(Component.translatable("tooltip.feedback.reads_off").withStyle(ChatFormatting.GOLD));
    }
}
