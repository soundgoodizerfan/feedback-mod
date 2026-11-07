// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.UpswingSwing;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** {@link SwingStartPayload}, relayed to everyone who can see the swinger (never the swinger, who played it already). */
public record SwingPlayPayload(int entity, String clip, int hand, float length, float upswing, String sound)
        implements CustomPacketPayload {

    public static final Type<SwingPlayPayload> TYPE = new Type<>(Feedback.id("swing_play"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwingPlayPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SwingPlayPayload::entity,
            ByteBufCodecs.STRING_UTF8, SwingPlayPayload::clip,
            ByteBufCodecs.VAR_INT, SwingPlayPayload::hand,
            ByteBufCodecs.FLOAT, SwingPlayPayload::length,
            ByteBufCodecs.FLOAT, SwingPlayPayload::upswing,
            ByteBufCodecs.STRING_UTF8, SwingPlayPayload::sound,
            SwingPlayPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SwingPlayPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> UpswingSwing.playRemote(payload));
    }
}
