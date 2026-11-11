// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.net.ToolScrollPayload;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Shift-scroll with a Data Connector or a hand hammer in the main hand changes its mode or
 * swing instead of the hotbar slot.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class ToolScroll {

    private ToolScroll() {
    }

    @SubscribeEvent
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        Player player = Minecraft.getInstance().player;
        if (player == null || Minecraft.getInstance().screen != null || !player.isShiftKeyDown()
                || !(player.getMainHandItem().getItem() instanceof io.github.soundgoodizerfan.feedback.item.DataConnectorItem
                        || io.github.soundgoodizerfan.feedback.modular.ModularHandTool.strikes(player.getMainHandItem()))
                || event.getScrollDeltaY() == 0)
            return;
        event.setCanceled(true);
        PacketDistributor.sendToServer(new ToolScrollPayload(event.getScrollDeltaY() > 0 ? -1 : 1));
    }
}
