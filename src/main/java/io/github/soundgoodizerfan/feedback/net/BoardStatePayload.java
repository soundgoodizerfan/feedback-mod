// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.village.BoardMenu;
import io.github.soundgoodizerfan.feedback.village.BoardView;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** What the open ledger board shows, sent on open and after each delivery. */
public record BoardStatePayload(int containerId, BoardView view) implements CustomPacketPayload {

    public static final Type<BoardStatePayload> TYPE = new Type<>(Feedback.id("board_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BoardStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BoardStatePayload::containerId,
            BoardView.STREAM_CODEC, BoardStatePayload::view,
            BoardStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BoardStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof BoardMenu menu && menu.containerId == payload.containerId())
                menu.accept(payload.view());
        });
    }
}
