// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.hit.SweptVolume;
import io.github.soundgoodizerfan.feedback.net.MeleeSweepPayload;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The client's prediction of the swept volume: when vanilla's pick finds nothing, run the same
 * {@link SweptVolume} the server will and tell the server only if it expects a hit. The crosshair
 * outline stays vanilla's (the pick), so a target reached only by the volume is not highlighted.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID, value = Dist.CLIENT)
public final class MeleeSweepClient {

    private MeleeSweepClient() {
    }

    @SubscribeEvent
    public static void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        sweepIfExpected(event.getEntity());
    }

    /** Tells the server of a swing the pick missed, only if the swept volume expects a hit. */
    public static void sweepIfExpected(net.minecraft.world.entity.player.Player player) {
        if (SweptVolume.halfWidth(player).filter(w -> !SweptVolume.targets(player, w).isEmpty()).isPresent())
            PacketDistributor.sendToServer(new MeleeSweepPayload());
    }
}
