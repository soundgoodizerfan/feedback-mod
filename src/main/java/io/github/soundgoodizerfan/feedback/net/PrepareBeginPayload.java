// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.PartBenchMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The player opened a Prep Table minigame: {@code kind} 0 is the cut, 1 the pour, 2 the knead. The menu notes when, so a result
 * cannot come back faster than it could be played ({@link PartBenchMenu#beginPrepare}).
 */
public record PrepareBeginPayload(int container, int kind) implements CustomPacketPayload {

    public static final Type<PrepareBeginPayload> TYPE = new Type<>(Feedback.id("prepare_begin"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PrepareBeginPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PrepareBeginPayload::container,
            ByteBufCodecs.VAR_INT, PrepareBeginPayload::kind,
            PrepareBeginPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PrepareBeginPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof PartBenchMenu menu
                    && menu.containerId == payload.container())
                menu.beginPrepare(player, payload.kind());
        });
    }
}
