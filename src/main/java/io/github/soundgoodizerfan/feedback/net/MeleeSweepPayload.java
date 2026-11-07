// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.combat.hit.MeleeSweep;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A melee swing at nothing the client's pick could see, that its swept volume predicts will hit ({@link MeleeSweep#miss}). */
public record MeleeSweepPayload() implements CustomPacketPayload {

    public static final Type<MeleeSweepPayload> TYPE = new Type<>(Feedback.id("melee_sweep"));

    public static final StreamCodec<ByteBuf, MeleeSweepPayload> STREAM_CODEC = StreamCodec.unit(new MeleeSweepPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MeleeSweepPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> MeleeSweep.miss((ServerPlayer) context.player()));
    }
}
