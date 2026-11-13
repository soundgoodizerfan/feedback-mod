// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.gun.CoinFx;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A flipped coin for the clients to draw (the coin itself is a point on the server's arc, never an entity): it goes up at
 * {@code (x, y, z)} on game tick {@code born} in the colour of its metal, until a later message with {@code gone} says it is struck or fallen.
 */
public record CoinFlipPayload(int id, boolean gone, double x, double y, double z, long born, int rgb) implements CustomPacketPayload {

    public static final Type<CoinFlipPayload> TYPE = new Type<>(Feedback.id("coin_flip"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CoinFlipPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                ByteBufCodecs.VAR_INT.encode(buf, p.id);
                buf.writeBoolean(p.gone);
                buf.writeDouble(p.x);
                buf.writeDouble(p.y);
                buf.writeDouble(p.z);
                buf.writeLong(p.born);
                buf.writeInt(p.rgb);
            },
            buf -> new CoinFlipPayload(ByteBufCodecs.VAR_INT.decode(buf), buf.readBoolean(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readLong(), buf.readInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerLevel level, int id, boolean gone, Vec3 at, long born, int rgb) {
        PacketDistributor.sendToPlayersNear(level, null, at.x, at.y, at.z, 64, new CoinFlipPayload(id, gone, at.x, at.y, at.z, born, rgb));
    }

    public static void handle(CoinFlipPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> CoinFx.on(payload));
    }
}
