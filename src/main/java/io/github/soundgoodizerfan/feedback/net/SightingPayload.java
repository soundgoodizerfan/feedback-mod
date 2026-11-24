// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.space.Sighting;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the player's telescope has resolved a planet; the server checks the sighting itself before it rewards anything. */
public record SightingPayload(String body) implements CustomPacketPayload {

    public static final Type<SightingPayload> TYPE = new Type<>(Feedback.id("sighting"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SightingPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.stringUtf8(32), SightingPayload::body, SightingPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SightingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player)
                Sighting.claim(player, payload.body());
        });
    }
}
