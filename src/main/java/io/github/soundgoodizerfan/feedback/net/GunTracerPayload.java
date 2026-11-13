// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.gun.GunFx;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A shot's streak for the clients to fly and draw: sent when a shot is fired, again when it bounces, and once at the
 * end. The client steps it with the server's drag and gravity, so between messages it follows the real bullet.
 *
 * @param id    the shot, so a later message finds it
 * @param ended the shot is over: stop at (x, y, z)
 * @param argb  the streak's colour; alpha says how bright
 * @param drag  the flight's drag multiplier
 */
public record GunTracerPayload(int id, boolean ended, double x, double y, double z, float vx, float vy, float vz,
                               int argb, float drag) implements CustomPacketPayload {

    public static final Type<GunTracerPayload> TYPE = new Type<>(Feedback.id("gun_tracer"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GunTracerPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.VAR_INT.encode(buf, p.id);
                buf.writeBoolean(p.ended);
                buf.writeDouble(p.x);
                buf.writeDouble(p.y);
                buf.writeDouble(p.z);
                buf.writeFloat(p.vx);
                buf.writeFloat(p.vy);
                buf.writeFloat(p.vz);
                buf.writeInt(p.argb);
                buf.writeFloat(p.drag);
            },
            buf -> new GunTracerPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readBoolean(), buf.readDouble(), buf.readDouble(),
                    buf.readDouble(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readInt(), buf.readFloat()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerLevel level, int id, boolean ended, Vec3 pos, Vec3 velocity, int argb, float drag) {
        PacketDistributor.sendToPlayersNear(level, null, pos.x, pos.y, pos.z, 160,
                new GunTracerPayload(id, ended, pos.x, pos.y, pos.z, (float) velocity.x, (float) velocity.y, (float) velocity.z, argb, drag));
    }

    public static void handle(GunTracerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> GunFx.tracer(payload));
    }
}
