/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
 * Shift-scroll with a Welder or a hand hammer in the main hand changes its mode or
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
                || !(player.getMainHandItem().is(FVehicles.WELDER.get())
                        || io.github.soundgoodizerfan.feedback.modular.ModularHandTool.strikes(player.getMainHandItem()))
                || event.getScrollDeltaY() == 0)
            return;
        event.setCanceled(true);
        PacketDistributor.sendToServer(new ToolScrollPayload(event.getScrollDeltaY() > 0 ? -1 : 1));
    }
}
