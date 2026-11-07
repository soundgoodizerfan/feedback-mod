// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.hit.BareMode;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The barehanded combat mode was switched ({@link BareMode}). */
public record BareModePayload(boolean on) implements CustomPacketPayload {

    public static final Type<BareModePayload> TYPE = new Type<>(Feedback.id("bare_mode"));

    public static final StreamCodec<ByteBuf, BareModePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(BareModePayload::new, BareModePayload::on);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BareModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> BareMode.set(context.player(), payload.on()));
    }
}
