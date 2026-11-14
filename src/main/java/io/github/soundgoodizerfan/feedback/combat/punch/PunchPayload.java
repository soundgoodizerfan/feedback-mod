// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.punch;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The punch key went down ({@code shock} false) or stayed down after a punch ({@code shock} true). The server decides everything. */
public record PunchPayload(boolean shock) implements CustomPacketPayload {

    public static final Type<PunchPayload> TYPE = new Type<>(Feedback.id("punch"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PunchPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, PunchPayload::shock, PunchPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PunchPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            if (payload.shock())
                Punching.shockwave(player);
            else
                Punching.punch(player);
        });
    }
}
