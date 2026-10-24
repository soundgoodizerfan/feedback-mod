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
