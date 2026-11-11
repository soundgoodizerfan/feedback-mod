// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.ClientPayloads;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server to client: the Engineering Table's output was taken and a hand-work game wants playing first. {@code game} is the game's id
 * ({@code client/MinigameScreens} maps it to a screen), {@code rounds} how many rounds (one per fastener for torque) and {@code quality}
 * the fastener quality 1 to 5, {@code data} what the game needs to draw itself (the fitting game's pieces: slot and seating rank pairs; empty for torque). The client answers with one {@link MinigameResultPayload}.
 */
public record MinigameOpenPayload(int container, String game, int rounds, int quality, String data) implements CustomPacketPayload {

    public static final Type<MinigameOpenPayload> TYPE = new Type<>(Feedback.id("minigame_open"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MinigameOpenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MinigameOpenPayload::container,
            ByteBufCodecs.stringUtf8(64), MinigameOpenPayload::game,
            ByteBufCodecs.VAR_INT, MinigameOpenPayload::rounds,
            ByteBufCodecs.VAR_INT, MinigameOpenPayload::quality,
            ByteBufCodecs.stringUtf8(128), MinigameOpenPayload::data,
            MinigameOpenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MinigameOpenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPayloads.openMinigame(payload));
    }
}
