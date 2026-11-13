// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.bench.PartBenchMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The player began a forging pattern (spec 4.1) in the Tool Table they have open: which menu and
 * which shape on its list. The menu notes when, and how hot the stock is, so it can later judge the
 * hits against the same heat clock the screen played and refuse a result sent faster than the
 * pattern could be played ({@link PartBenchMenu#beginForge}). A payload naming a menu that is not
 * the one open is ignored.
 */
public record ForgeBeginPayload(int container, int option) implements CustomPacketPayload {

    public static final Type<ForgeBeginPayload> TYPE = new Type<>(Feedback.id("forge_begin"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ForgeBeginPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ForgeBeginPayload::container,
            ByteBufCodecs.VAR_INT, ForgeBeginPayload::option,
            ForgeBeginPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ForgeBeginPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof PartBenchMenu menu
                    && menu.containerId == payload.container())
                menu.beginForge(player, payload.option());
        });
    }
}
