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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * R went down ({@code held}) or came up on a held gun. The server runs the reload itself and picks
 * every round and magazine from the player's own inventory; the client only says it is holding.
 */
public record GunReloadPayload(boolean held) implements CustomPacketPayload {

    public static final Type<GunReloadPayload> TYPE = new Type<>(Feedback.id("gun_reload"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GunReloadPayload> STREAM_CODEC =
            net.minecraft.network.codec.ByteBufCodecs.BOOL.map(GunReloadPayload::new, GunReloadPayload::held).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GunReloadPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (payload.held())
                GunShots.reloadStart(context.player(), System.currentTimeMillis());
            else
                GunShots.reloadStop(context.player());
        });
    }
}
