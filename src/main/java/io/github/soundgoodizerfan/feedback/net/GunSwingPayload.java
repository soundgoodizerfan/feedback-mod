// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.weapon.GunBludgeon;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The client swung a gun in the melee stance; {@code offHand} says which gun. The server finds the hit. */
public record GunSwingPayload(boolean offHand) implements CustomPacketPayload {

    public static final Type<GunSwingPayload> TYPE = new Type<>(Feedback.id("gun_swing"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GunSwingPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, GunSwingPayload::offHand, GunSwingPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GunSwingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && !player.isSpectator())
                GunBludgeon.swing(player, payload.offHand());
        });
    }
}
