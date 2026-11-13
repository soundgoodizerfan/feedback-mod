// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.part;

import io.github.soundgoodizerfan.feedback.Feedback;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A pack key was pressed: the server opens the worn pack ({@link ModularBackpack#open}), or its crafting pad (action {@link #PAD}). */
public record PackPayload(byte action) implements CustomPacketPayload {

    public static final byte OPEN = 0, PAD = 1, DROP = 2, SLEEP = 3;

    public static final Type<PackPayload> TYPE = new Type<>(Feedback.id("open_pack"));

    public static final StreamCodec<ByteBuf, PackPayload> STREAM_CODEC = StreamCodec.of((buf, p) -> buf.writeByte(p.action()), buf -> new PackPayload(buf.readByte()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PackPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer p = (ServerPlayer) context.player();
            switch (payload.action()) {
                case PAD -> ModularBackpack.openPad(p);
                case DROP -> ModularBackpack.release(p);
                case SLEEP -> ModularBackpack.sleep(p);
                default -> ModularBackpack.open(p);
            }
        });
    }
}
