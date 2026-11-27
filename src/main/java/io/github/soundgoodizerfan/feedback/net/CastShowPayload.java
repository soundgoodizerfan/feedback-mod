// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.combat.CastVisuals;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * One procedural effect of a cast technique, drawn by {@link CastVisuals}: the client builds the shape from the
 * kind, two points, a colour, a size, a lifetime and a seed, so nothing is sent per frame.
 *
 * @param kind   a {@code CastVisuals} kind constant
 * @param entity an entity the effect follows (id), or -1; with one, {@code a} is an offset from its feet
 * @param a      first point (start, centre or offset)
 * @param b      second point or direction, kind-specific
 * @param rgb    colour
 * @param scale  size in blocks (radius, length or width, kind-specific)
 * @param life   lifetime in ticks
 */
public record CastShowPayload(byte kind, int entity, double ax, double ay, double az, float bx, float by, float bz,
                              int rgb, float scale, short life, long seed) implements CustomPacketPayload {

    public static final Type<CastShowPayload> TYPE = new Type<>(Feedback.id("cast_show"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CastShowPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeByte(p.kind);
                ByteBufCodecs.VAR_INT.encode(buf, p.entity + 1);
                buf.writeDouble(p.ax);
                buf.writeDouble(p.ay);
                buf.writeDouble(p.az);
                buf.writeFloat(p.bx);
                buf.writeFloat(p.by);
                buf.writeFloat(p.bz);
                buf.writeInt(p.rgb);
                buf.writeFloat(p.scale);
                buf.writeShort(p.life);
                buf.writeLong(p.seed);
            },
            buf -> new CastShowPayload(buf.readByte(), ByteBufCodecs.VAR_INT.decode(buf) - 1, buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readInt(), buf.readFloat(), buf.readShort(), buf.readLong()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Sends the effect to everyone within 96 blocks of {@code near}. */
    public static void send(ServerLevel level, byte kind, int entity, Vec3 near, Vec3 a, Vec3 b, int rgb, float scale, int life) {
        PacketDistributor.sendToPlayersNear(level, null, near.x, near.y, near.z, 96,
                new CastShowPayload(kind, entity, a.x, a.y, a.z, (float) b.x, (float) b.y, (float) b.z, rgb, scale, (short) life, level.random.nextLong()));
    }

    public static void handle(CastShowPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> CastVisuals.show(payload));
    }
}
