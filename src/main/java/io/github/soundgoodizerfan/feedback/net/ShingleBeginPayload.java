// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ToolTableMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The player began shingling the bloom in the centre of the bench they have open; the menu notes when and how hot it is ({@link ToolTableMenu#beginShingle}). */
public record ShingleBeginPayload(int container) implements CustomPacketPayload {

    public static final Type<ShingleBeginPayload> TYPE = new Type<>(Feedback.id("shingle_begin"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShingleBeginPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ShingleBeginPayload::container, ShingleBeginPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ShingleBeginPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof ToolTableMenu menu
                    && menu.containerId == payload.container())
                menu.beginShingle(player);
        });
    }
}
