// Copyright (C) 2026 soundgoodizerfan/feedback
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.suit;

import com.mojang.blaze3d.platform.InputConstants;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.net.SuitPayload;
import io.github.soundgoodizerfan.feedback.suit.Ability;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/** The suit's keys: hold the wheel key to open the ability wheel (let go over a segment to switch it), hold the dash key to thrust, tap the screen key for the diagnostic. */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class SuitKeys {

    public static final KeyMapping WHEEL = new KeyMapping("key.feedback.suit_wheel", InputConstants.KEY_V, "key.categories.feedback");
    public static final KeyMapping DASH = new KeyMapping("key.feedback.suit_dash", InputConstants.KEY_B, "key.categories.feedback");
    public static final KeyMapping JET = new KeyMapping("key.feedback.suit_jet", InputConstants.KEY_G, "key.categories.feedback");
    public static final KeyMapping SCREEN = new KeyMapping("key.feedback.suit_screen", InputConstants.KEY_J, "key.categories.feedback");

    private static boolean dash, jet;

    private SuitKeys() {
    }

    @SubscribeEvent
    public static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            SuitClient.clear();
            return;
        }
        boolean worn = SuitClient.worn();
        while (WHEEL.consumeClick())
            if (worn && mc.screen == null)
                mc.setScreen(new SuitWheelScreen());
        while (SCREEN.consumeClick())
            if (worn && mc.screen == null)
                mc.setScreen(new SuitScreen());
        boolean want = worn && mc.screen == null && DASH.isDown() && SuitClient.live(Ability.JUMP_DASH);
        if (want != dash) {
            dash = want;
            PacketDistributor.sendToServer(new SuitPayload(SuitPayload.DASH, want ? 1 : 0));
        }
        boolean lift = worn && mc.screen == null && JET.isDown() && SuitClient.live(Ability.JET);
        if (lift != jet) {
            jet = lift;
            PacketDistributor.sendToServer(new SuitPayload(SuitPayload.JET, lift ? 1 : 0));
        }
    }
}
