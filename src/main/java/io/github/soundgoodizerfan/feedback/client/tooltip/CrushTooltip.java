// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.tooltip;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** A line on any stack that has been ground in the brewing stand's bowl: how far. */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class CrushTooltip {

    private CrushTooltip() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Float fraction = event.getItemStack().get(FDataComponents.CRUSHED_FRACTION.get());
        if (fraction != null)
            event.getToolTip().add(Component.translatable("tooltip.feedback.ground", Math.round(fraction * 100f)));
    }
}
