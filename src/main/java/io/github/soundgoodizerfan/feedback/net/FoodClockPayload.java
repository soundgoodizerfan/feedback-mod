// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.FoodClock;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The ticks the server has spent empty, sent at login so the client reads food stamps on {@link FoodClock}'s clock. */
public record FoodClockPayload(long empty) implements CustomPacketPayload {

    public static final Type<FoodClockPayload> TYPE = new Type<>(Feedback.id("food_clock"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FoodClockPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.VAR_LONG, FoodClockPayload::empty, FoodClockPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FoodClockPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> FoodClock.setClient(payload.empty()));
    }
}
