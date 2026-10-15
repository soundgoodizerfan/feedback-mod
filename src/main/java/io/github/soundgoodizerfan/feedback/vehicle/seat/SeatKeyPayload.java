/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
