// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.parry;

import io.github.soundgoodizerfan.feedback.Feedback;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The parry key was pressed. The server decides whether it counts ({@link Parrying#begin}). */
public record ParryPayload() implements CustomPacketPayload {

    public static final Type<ParryPayload> TYPE = new Type<>(Feedback.id("parry"));

    public static final StreamCodec<ByteBuf, ParryPayload> STREAM_CODEC = StreamCodec.unit(new ParryPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ParryPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Parrying.begin((ServerPlayer) context.player()));
    }
}
