// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.HudReadouts;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A short line for the HUD readout stack ({@link HudReadouts}) in place of the vanilla action bar:
 * the text, how long it stays in ticks, and a priority that decides which of a full stack is
 * dropped first. Sent through {@link #send}.
 */
public record HudReadoutPayload(Component text, int ticks, int priority) implements CustomPacketPayload {

    public static final Type<HudReadoutPayload> TYPE = new Type<>(Feedback.id("hud_readout"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HudReadoutPayload> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC, HudReadoutPayload::text,
            ByteBufCodecs.VAR_INT, HudReadoutPayload::ticks,
            ByteBufCodecs.VAR_INT, HudReadoutPayload::priority,
            HudReadoutPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Server side: show {@code text} on {@code player}'s HUD for {@code ticks}. */
    public static void send(ServerPlayer player, Component text, int ticks) {
        PacketDistributor.sendToPlayer(player, new HudReadoutPayload(text, ticks, 0));
    }

    public static void handle(HudReadoutPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> HudReadouts.add(payload.text(), payload.ticks(), payload.priority()));
    }
}
