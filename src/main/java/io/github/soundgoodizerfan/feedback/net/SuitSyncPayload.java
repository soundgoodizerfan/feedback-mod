// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * What the wearer's suit looks like right now, for its HUD, wheel and screens: sent only when a figure moved by a visible step (charge by
 * a sixty-fourth, heat by a degree, any toggle), never every tick. {@code worn} false clears the client's copy.
 *
 * @param available the abilities the fittings allow ({@code Ability#bit})
 * @param toggles   the abilities that are on
 * @param charge    the bay battery's state of charge, 0 to 1
 * @param heat      Tu of the suit
 * @param gauge     the gauges' step factor (0 none, else 0.65 to 1)
 * @param draw      amps being drawn now
 */
public record SuitSyncPayload(boolean worn, int available, int toggles, float charge, boolean battery, float heat, boolean cut,
                              boolean overload, float gauge, float mass, float draw, float limit) implements CustomPacketPayload {

    public static final Type<SuitSyncPayload> TYPE = new Type<>(Feedback.id("suit_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SuitSyncPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBoolean(p.worn());
                buf.writeVarInt(p.available());
                buf.writeVarInt(p.toggles());
                buf.writeFloat(p.charge());
                buf.writeBoolean(p.battery());
                buf.writeFloat(p.heat());
                buf.writeBoolean(p.cut());
                buf.writeBoolean(p.overload());
                buf.writeFloat(p.gauge());
                buf.writeFloat(p.mass());
                buf.writeFloat(p.draw());
                buf.writeFloat(p.limit());
            },
            buf -> new SuitSyncPayload(buf.readBoolean(), buf.readVarInt(), buf.readVarInt(), buf.readFloat(), buf.readBoolean(),
                    buf.readFloat(), buf.readBoolean(), buf.readBoolean(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat()));

    public static final SuitSyncPayload OFF = new SuitSyncPayload(false, 0, 0, 0, false, 0, false, false, 0, 0, 0, 0);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SuitSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> io.github.soundgoodizerfan.feedback.client.SuitClient.apply(payload));
    }
}
