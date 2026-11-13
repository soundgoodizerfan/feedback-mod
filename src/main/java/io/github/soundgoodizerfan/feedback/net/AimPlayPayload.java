// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** {@link AimPayload}, relayed to everyone who can see the aimer. */
public record AimPlayPayload(int entity, boolean aiming) implements CustomPacketPayload {

    public static final Type<AimPlayPayload> TYPE = new Type<>(Feedback.id("aim_play"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AimPlayPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, AimPlayPayload::entity,
            ByteBufCodecs.BOOL, AimPlayPayload::aiming, AimPlayPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AimPlayPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> io.github.soundgoodizerfan.feedback.client.gun.GunClient.setAimed(payload.entity(), payload.aiming()));
    }
}
