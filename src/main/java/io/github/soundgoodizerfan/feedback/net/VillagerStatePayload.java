// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.village.VillagerMenu;
import io.github.soundgoodizerfan.feedback.village.VillagerView;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** What the open villager screen shows, sent on open and after each delivery or purchase. */
public record VillagerStatePayload(int containerId, VillagerView view) implements CustomPacketPayload {

    public static final Type<VillagerStatePayload> TYPE = new Type<>(Feedback.id("villager_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, VillagerStatePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VillagerStatePayload::containerId,
            VillagerView.STREAM_CODEC, VillagerStatePayload::view,
            VillagerStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(VillagerStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof VillagerMenu menu && menu.containerId == payload.containerId())
                menu.accept(payload.view());
        });
    }
}
