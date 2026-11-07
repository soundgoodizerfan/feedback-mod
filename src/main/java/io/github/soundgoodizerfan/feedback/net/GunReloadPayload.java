// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
