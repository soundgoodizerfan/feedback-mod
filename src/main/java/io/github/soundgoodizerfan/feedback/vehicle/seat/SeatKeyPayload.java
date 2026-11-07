// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.seat;

import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A key went down or came up under a seated driver. No position: the server takes the seat from
 * what the player is riding, so a client can only press keys on the seat it sits in.
 */
public record SeatKeyPayload(int key, boolean down) implements CustomPacketPayload {

    public static final Type<SeatKeyPayload> TYPE = new Type<>(Feedback.id("seat_key"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SeatKeyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SeatKeyPayload::key,
            ByteBufCodecs.BOOL, SeatKeyPayload::down,
            SeatKeyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SeatKeyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().getVehicle() instanceof DriverSeatEntity seat
                    && seat.level().getBlockEntity(seat.blockPosition()) instanceof DriverSeatBlockEntity be)
                be.key(payload.key(), payload.down());
        });
    }
}
