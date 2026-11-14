// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.whip;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The whip key went down ({@code down} true: throw, or cancel a line that is out) or came up (reel in). The server decides everything. */
public record WhipPayload(boolean down) implements CustomPacketPayload {

    public static final Type<WhipPayload> TYPE = new Type<>(Feedback.id("whip"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WhipPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, WhipPayload::down, WhipPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WhipPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (payload.down())
                Whipping.press(player);
            else
                Whipping.release(player);
        });
    }
}
