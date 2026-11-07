// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.potion.BrewingStandMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The player took a plant out of the grinding bowl (or closed the screen with it still there): which place, and how far it was crushed in twentieths. The server clamps both and hands the plant back. */
public record MortarTakePayload(int containerId, int place, int steps) implements CustomPacketPayload {

    public static final Type<MortarTakePayload> TYPE = new Type<>(Feedback.id("mortar_take"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MortarTakePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MortarTakePayload::containerId,
            ByteBufCodecs.VAR_INT, MortarTakePayload::place,
            ByteBufCodecs.VAR_INT, MortarTakePayload::steps,
            MortarTakePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MortarTakePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof BrewingStandMenu menu && menu.containerId == payload.containerId())
                menu.mortarTake(payload.place(), payload.steps());
        });
    }
}
