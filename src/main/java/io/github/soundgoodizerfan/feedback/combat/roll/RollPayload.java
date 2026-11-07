// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.roll;

import io.github.soundgoodizerfan.feedback.Feedback;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The roll key was pressed, with the movement input held at that moment (forward and strafe, left positive). */
public record RollPayload(float forward, float strafe) implements CustomPacketPayload {

    public static final Type<RollPayload> TYPE = new Type<>(Feedback.id("roll"));

    public static final StreamCodec<ByteBuf, RollPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, RollPayload::forward,
            ByteBufCodecs.FLOAT, RollPayload::strafe,
            RollPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RollPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Rolling.begin((ServerPlayer) context.player(),
                Mth.clamp(payload.forward(), -1f, 1f), Mth.clamp(payload.strafe(), -1f, 1f), true));
    }
}
