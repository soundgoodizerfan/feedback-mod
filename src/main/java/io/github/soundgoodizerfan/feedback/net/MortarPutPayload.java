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

/** Put one plant into the brewing stand's grinding bowl: from the menu slot named, or from the cursor if the slot is -1. The server moves it; the bowl's items sync back as slots. */
public record MortarPutPayload(int containerId, int slot) implements CustomPacketPayload {

    public static final Type<MortarPutPayload> TYPE = new Type<>(Feedback.id("mortar_put"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MortarPutPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MortarPutPayload::containerId,
            ByteBufCodecs.VAR_INT, MortarPutPayload::slot,
            MortarPutPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MortarPutPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof BrewingStandMenu menu && menu.containerId == payload.containerId())
                menu.mortarPut(payload.slot());
        });
    }
}
