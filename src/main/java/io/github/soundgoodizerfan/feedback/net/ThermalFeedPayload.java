// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * What a thermal-vision wearer can see warm or cold: the temperatures of the things in range, a few times a second and only while the
 * view is on. Block bodies and hot items carry their real {@code Tu}; a mob carries the figure of its body-heat class; lava and fire carry
 * theirs. The client draws them; it decides nothing.
 */
public record ThermalFeedPayload(List<Entry> entries) implements CustomPacketPayload {

    public static final byte BLOCK = 0, ITEM = 1, MOB = 2, FLUID = 3;

    /** One warm or cold thing: where it is, its temperature, which of the four kinds it is and how big it looks. */
    public record Entry(float x, float y, float z, float tu, byte kind, float size) {
    }

    public static final Type<ThermalFeedPayload> TYPE = new Type<>(Feedback.id("thermal_feed"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ThermalFeedPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.entries().size());
                for (Entry e : p.entries()) {
                    buf.writeFloat(e.x());
                    buf.writeFloat(e.y());
                    buf.writeFloat(e.z());
                    buf.writeFloat(e.tu());
                    buf.writeByte(e.kind());
                    buf.writeFloat(e.size());
                }
            },
            buf -> {
                int n = buf.readVarInt();
                List<Entry> out = new ArrayList<>(n);
                for (int i = 0; i < n; i++)
                    out.add(new Entry(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readByte(), buf.readFloat()));
                return new ThermalFeedPayload(out);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ThermalFeedPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> io.github.soundgoodizerfan.feedback.client.ThermalView.apply(payload.entries()));
    }
}
