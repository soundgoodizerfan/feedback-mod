// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.tooltip;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.xp.XpStore;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * What a stack holding experience feels like in the hand: a level word and a fill word, never a figure
 * ({@link Readout#xpLevel}, {@link Readout#xpFill}; the numbers are an instrument's).
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public class XpTooltip {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        XpStore s = event.getItemStack().get(FDataComponents.XP_STORE.get());
        if (s == null)
            return;
        if (Readout.instrumented(Quantity.XP_LEVEL)) {
            event.getToolTip().add(Readout.xpFigures(s.level(), s.points(), s.maxPoints()));
            return;
        }
        event.getToolTip().add(s.points() == 0 ? Readout.xpFill(s)
                : Component.translatable("tooltip.feedback.xp.held", Readout.xpLevel(s), Readout.xpFill(s)).withStyle(ChatFormatting.GRAY));
    }
}
