// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.tooltip;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FCombat;
import io.github.soundgoodizerfan.feedback.registry.FItems;
import io.github.soundgoodizerfan.feedback.space.SkyBodies;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** The sky notebook lists the planets its reader has resolved through a telescope, each with its colour, what its air showed and one fact; the rest are blank lines. */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class SkyNotebookTooltip {

    private SkyNotebookTooltip() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!event.getItemStack().is(FItems.SKY_NOTEBOOK.get()))
            return;
        var player = Minecraft.getInstance().player;
        var seen = player == null ? java.util.List.<String>of() : player.getData(FCombat.SIGHTED);
        for (SkyBodies.Body body : SkyBodies.ALL) {
            if (seen.contains(body.id())) {
                event.getToolTip().add(Component.translatable("planet.feedback." + body.id()).withStyle(ChatFormatting.GOLD));
                event.getToolTip().add(Component.translatable("tooltip.feedback.sky_notebook." + body.id()).withStyle(ChatFormatting.GRAY));
            } else
                event.getToolTip().add(Component.translatable("tooltip.feedback.sky_notebook.blank").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
