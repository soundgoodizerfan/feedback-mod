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
 * Where a bullet landed or a gun fired, for the clients to dress: particles, decals, flash. Purely visual; the
 * server decided the hit already.
 *
 * @param kind   {@link #FLESH}, {@link #BONE}, {@link #ARMOUR}, {@link #BLOCK}, {@link #RICOCHET} or {@link #MUZZLE}
 * @param dir    the surface normal (a block or ricochet), the direction of travel (a body), or the aim (a muzzle)
 * @param power  0 to 1, how hard the hit or shot was
 * @param data   a block state id (block, ricochet), a colour (flesh), or 1 for a muzzle that ejects a case
 * @param decal  {@code GunFx} decal kind, or -1 for none
 */
public record GunImpactPayload(byte kind, double x, double y, double z, float dx, float dy, float dz,
                               float power, int data, byte decal) implements CustomPacketPayload {

    public static final byte FLESH = 0, BONE = 1, ARMOUR = 2, BLOCK = 3, RICOCHET = 4, MUZZLE = 5;

    public static final Type<GunImpactPayload> TYPE = new Type<>(Feedback.id("gun_impact"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GunImpactPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeByte(p.kind);
                buf.writeDouble(p.x);
                buf.writeDouble(p.y);
                buf.writeDouble(p.z);
                buf.writeFloat(p.dx);
                buf.writeFloat(p.dy);
                buf.writeFloat(p.dz);
                buf.writeFloat(p.power);
                ByteBufCodecs.VAR_INT.encode(buf, p.data);
                buf.writeByte(p.decal);
            },
            buf -> new GunImpactPayload(buf.readByte(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    ByteBufCodecs.VAR_INT.decode(buf), buf.readByte()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Server side: send to everyone within sight of {@code at}. */
    public static void send(ServerLevel level, byte kind, Vec3 at, Vec3 dir, float power, int data, int decal) {
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, 96,
                new GunImpactPayload(kind, at.x, at.y, at.z, (float) dir.x, (float) dir.y, (float) dir.z,
                        Math.min(1f, Math.max(0f, power)), data, (byte) decal));
    }

    public static void handle(GunImpactPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> GunFx.impact(payload));
    }
}
