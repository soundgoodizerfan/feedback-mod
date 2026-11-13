// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.screen;

import com.mojang.blaze3d.platform.InputConstants;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.part.PackPayload;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** The keys that open the worn pack and its crafting pad; the server finds the pack and decides. */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class PackKey {

    /** The pack's own screen: the chest screen, over {@link PackMenu}'s typed slots. */
    @EventBusSubscriber(modid = Feedback.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Screens {
        @SubscribeEvent
        public static void register(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
            event.register(io.github.soundgoodizerfan.feedback.registry.FMenus.PACK.get(), PackScreen::new);
        }

        @SubscribeEvent
        public static void keys(net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event) {
            event.register(PAD_KEY);
            event.register(DROP_KEY);
            event.register(SLEEP_KEY);
        }
    }


    public static final KeyMapping KEY = new KeyMapping("key.feedback.open_pack", InputConstants.KEY_B, "key.categories.feedback");

    public static final KeyMapping PAD_KEY = new KeyMapping("key.feedback.open_pack_pad", InputConstants.KEY_N, "key.categories.feedback");

    public static final KeyMapping DROP_KEY = new KeyMapping("key.feedback.drop_pack", InputConstants.UNKNOWN.getValue(), "key.categories.feedback");
    public static final KeyMapping SLEEP_KEY = new KeyMapping("key.feedback.pack_bedroll", InputConstants.KEY_COMMA, "key.categories.feedback");

    private PackKey() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (KEY.consumeClick())
            if (mc.player != null && mc.screen == null)
                PacketDistributor.sendToServer(new PackPayload(PackPayload.OPEN));
        while (DROP_KEY.consumeClick())
            if (mc.player != null && mc.screen == null)
                PacketDistributor.sendToServer(new PackPayload(PackPayload.DROP));
        while (SLEEP_KEY.consumeClick())
            if (mc.player != null && mc.screen == null)
                PacketDistributor.sendToServer(new PackPayload(PackPayload.SLEEP));
        while (PAD_KEY.consumeClick())
            if (mc.player != null && mc.screen == null)
                PacketDistributor.sendToServer(new PackPayload(PackPayload.PAD));
    }
}
