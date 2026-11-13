// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.gun.GunFx;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** To the shooter only: their bullet hit a living thing (a tick) or killed it (a deeper one). */
public record GunHitMarkPayload(boolean kill) implements CustomPacketPayload {

    public static final Type<GunHitMarkPayload> TYPE = new Type<>(Feedback.id("gun_hit_mark"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GunHitMarkPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, GunHitMarkPayload::kill, GunHitMarkPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, boolean kill) {
        PacketDistributor.sendToPlayer(player, new GunHitMarkPayload(kill));
    }

    public static void handle(GunHitMarkPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> GunFx.hitMark(payload.kill()));
    }
}
