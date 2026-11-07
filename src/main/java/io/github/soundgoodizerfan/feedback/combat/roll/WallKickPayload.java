// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.roll;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Jump was pressed in the air against a wall. The server checks everything; this only says it happened. */
public record WallKickPayload() implements CustomPacketPayload {

    public static final Type<WallKickPayload> TYPE = new Type<>(Feedback.id("wall_kick"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WallKickPayload> STREAM_CODEC = StreamCodec.unit(new WallKickPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WallKickPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> WallKicking.kick((ServerPlayer) context.player()));
    }
}
