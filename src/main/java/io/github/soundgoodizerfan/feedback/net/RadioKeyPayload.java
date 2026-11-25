// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.radio.RadioNet;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client to server: the push-to-talk key went down or up. Only counts while the person holds a radio set. */
public record RadioKeyPayload(boolean down) implements CustomPacketPayload {

    public static final Type<RadioKeyPayload> TYPE = new Type<>(Feedback.id("radio_key"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RadioKeyPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, RadioKeyPayload::down, RadioKeyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RadioKeyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player)
                RadioNet.key(player, payload.down());
        });
    }
}
