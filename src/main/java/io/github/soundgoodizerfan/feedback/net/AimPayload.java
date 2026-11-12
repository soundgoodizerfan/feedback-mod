// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A player started or stopped aiming a gun. Pure looks: the server relays it to onlookers, who raise the gun in third person. */
public record AimPayload(boolean aiming) implements CustomPacketPayload {

    public static final Type<AimPayload> TYPE = new Type<>(Feedback.id("aim"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AimPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, AimPayload::aiming, AimPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AimPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                var held = player.getMainHandItem();
                if (held.getItem() instanceof io.github.soundgoodizerfan.feedback.weapon.GunItem gun)
                    gun.mechanism().aimChanged(held, payload.aiming(), player.level().getGameTime());
            }
            if (context.player() instanceof ServerPlayer player)
                PacketDistributor.sendToPlayersTrackingEntity(player, new AimPlayPayload(player.getId(), payload.aiming()));
        });
    }
}
