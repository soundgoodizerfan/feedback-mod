// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.engineering.EngineeringTableMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client to server: how the Engineering Table's hand-work game went, one packet per assembly, a score in percent (0..100). The menu
 * checks it asked for a game, clamps the number, scores a result that came faster than it could be played as nothing, and uses it only to
 * move the start condition ({@link EngineeringTableMenu#finishMinigame}); a poor score never blocks the craft.
 */
public record MinigameResultPayload(int container, int percent) implements CustomPacketPayload {

    public static final Type<MinigameResultPayload> TYPE = new Type<>(Feedback.id("minigame_result"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MinigameResultPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MinigameResultPayload::container,
            ByteBufCodecs.VAR_INT, MinigameResultPayload::percent,
            MinigameResultPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MinigameResultPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof EngineeringTableMenu menu
                    && menu.containerId == payload.container())
                menu.finishMinigame(player, payload.percent());
        });
    }
}
