// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.hit.OffhandStrike;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The off-hand attack key was pressed, or in barehanded mode either fist struck ({@link OffhandStrike}). */
public record OffhandStrikePayload(boolean main) implements CustomPacketPayload {

    public static final Type<OffhandStrikePayload> TYPE = new Type<>(Feedback.id("offhand_strike"));

    public static final StreamCodec<ByteBuf, OffhandStrikePayload> STREAM_CODEC = ByteBufCodecs.BOOL.map(OffhandStrikePayload::new, OffhandStrikePayload::main);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OffhandStrikePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> OffhandStrike.strike((ServerPlayer) context.player(), payload.main()));
    }
}
