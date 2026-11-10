// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.suit.SuitServer;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The wearer asks the suit for something: switch an ability ({@link #TOGGLE}, arg the ability's ordinal), hold or let go of the dash
 * ({@link #DASH}, arg 1 or 0), put the battery from an inventory slot into the bay ({@link #BAY_PUT}, arg the slot) or take it out
 * ({@link #BAY_TAKE}). The server checks every one against what is worn; the client only asks.
 */
public record SuitPayload(byte action, int arg) implements CustomPacketPayload {

    public static final byte TOGGLE = 0, DASH = 1, BAY_PUT = 2, BAY_TAKE = 3;

    public static final Type<SuitPayload> TYPE = new Type<>(Feedback.id("suit"));

    public static final StreamCodec<ByteBuf, SuitPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeByte(p.action());
                buf.writeInt(p.arg());
            },
            buf -> new SuitPayload(buf.readByte(), buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SuitPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SuitServer.handle((ServerPlayer) context.player(), payload.action(), payload.arg()));
    }
}
