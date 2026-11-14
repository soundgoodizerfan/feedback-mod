// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client.combat;

import com.mojang.blaze3d.platform.InputConstants;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.gear.CombatGear;
import io.github.soundgoodizerfan.feedback.combat.punch.Punching;
import io.github.soundgoodizerfan.feedback.combat.tether.GrappleHook;
import io.github.soundgoodizerfan.feedback.combat.whip.WhipPayload;
import io.github.soundgoodizerfan.feedback.combat.whip.WhipTuning;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Whiplash's key. Down throws the hook (or lets go of a line that is out), up tells the server to reel; while the line it holds is a
 * swing (a block, or a creature too heavy to drag), {@link #winching} makes {@link GrappleSwing} draw the wearer in along it, as holding
 * use does for a grapple in hand. The wearer's momentum stays when the line ends.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class WhipClient {

    public static final KeyMapping WHIP = new KeyMapping("key.feedback.whip", InputConstants.KEY_B, "key.categories.feedback");

    private static boolean down, winching;

    private WhipClient() {
    }

    /** Whether the key has come up on a line that holds, and the reel is drawing the wearer in. */
    public static boolean winching() {
        return winching;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.level == null)
            return;
        while (WHIP.consumeClick()) {
        }
        boolean held = mc.screen == null && !mc.isPaused() && !player.isSpectator() && WHIP.isDown()
                && !CombatGear.granting(player, WhipTuning.WHIPLASH).isEmpty();
        var hooks = GrappleHook.allOf(mc.level, player).stream().filter(h -> h.has(GrappleHook.WHIP)).toList();
        if (held && !down) {
            // A line out is let go; none out, the hook is thrown.
            winching = false;
            PacketDistributor.sendToServer(new WhipPayload(true));
            if (hooks.isEmpty())
                player.swing(Punching.hand(player));
        } else if (!held && down) {
            PacketDistributor.sendToServer(new WhipPayload(false));
            winching = hooks.stream().anyMatch(GrappleHook::anchored);
        }
        down = held;
        if (winching && hooks.stream().noneMatch(GrappleHook::anchored))
            winching = false;
    }
}
