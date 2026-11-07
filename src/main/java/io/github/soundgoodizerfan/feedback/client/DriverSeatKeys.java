// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.vehicle.seat.DriverSeatEntity;
import io.github.soundgoodizerfan.feedback.vehicle.seat.SeatKeyPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * While sitting in a Driver's Seat the keyboard is the seat's: every press and release goes to
 * the server ({@link SeatKeyPayload}) and the key mapping it would have triggered is swallowed,
 * the Linked Typewriter's way (Create Aeronautics, MIT; design only). Kept for the player:
 * sneak (to get up), Escape, chat and the function keys.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class DriverSeatKeys {

    private DriverSeatKeys() {
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null || !(mc.player.getVehicle() instanceof DriverSeatEntity))
            return;
        int key = event.getKey();
        var options = mc.options;
        if (key == GLFW.GLFW_KEY_ESCAPE || (key >= GLFW.GLFW_KEY_F1 && key <= GLFW.GLFW_KEY_F25)
                || options.keyShift.matches(key, event.getScanCode()) || options.keyChat.matches(key, event.getScanCode())
                || options.keyCommand.matches(key, event.getScanCode()))
            return;
        if (event.getAction() != GLFW.GLFW_REPEAT)
            PacketDistributor.sendToServer(new SeatKeyPayload(key, event.getAction() == GLFW.GLFW_PRESS));
        for (KeyMapping mapping : options.keyMappings)
            if (mapping.matches(key, event.getScanCode())) {
                while (mapping.consumeClick()) {
                }
                mapping.setDown(false);
            }
    }
}
