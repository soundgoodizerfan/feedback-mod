// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.gun.GunFx;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * One instant beam of an energy weapon, from {@code from} to {@code to}, for the clients to draw for a few ticks. Purely visual.
 *
 * @param style an {@code EnergyStyle}
 * @param rgb   the beam's colour
 * @param width 1 for a plain beam, less for one of a splitter
 */
public record EnergyBeamPayload(byte style, int rgb, double fx, double fy, double fz, double tx, double ty, double tz,
                                float width) implements CustomPacketPayload {

    public static final Type<EnergyBeamPayload> TYPE = new Type<>(Feedback.id("energy_beam"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnergyBeamPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeByte(p.style);
                buf.writeInt(p.rgb);
                buf.writeDouble(p.fx);
                buf.writeDouble(p.fy);
                buf.writeDouble(p.fz);
                buf.writeDouble(p.tx);
                buf.writeDouble(p.ty);
                buf.writeDouble(p.tz);
                buf.writeFloat(p.width);
            },
            buf -> new EnergyBeamPayload(buf.readByte(), buf.readInt(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerLevel level, byte style, int rgb, Vec3 from, Vec3 to, float width) {
        PacketDistributor.sendToPlayersNear(level, null, from.x, from.y, from.z, 160,
                new EnergyBeamPayload(style, rgb, from.x, from.y, from.z, to.x, to.y, to.z, width));
    }

    public static void handle(EnergyBeamPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> GunFx.beam(payload));
    }
}
