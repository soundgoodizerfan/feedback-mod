// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.radio;

import com.mojang.blaze3d.platform.InputConstants;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.net.RadioKeyPayload;
import io.github.soundgoodizerfan.feedback.radio.RadioSets;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** The push-to-talk key of the radio set: the server is told when it goes down or up while a set is held, and when the set is put away. */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class RadioKeys {

    public static final KeyMapping TALK = new KeyMapping("key.feedback.radio_talk", InputConstants.KEY_B, "key.categories.feedback");
    private static boolean sent;

    private RadioKeys() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        var player = Minecraft.getInstance().player;
        boolean down = player != null && Minecraft.getInstance().screen == null && TALK.isDown() && !RadioSets.held(player).isEmpty();
        if (down != sent) {
            sent = down;
            PacketDistributor.sendToServer(new RadioKeyPayload(down));
        }
    }
}
