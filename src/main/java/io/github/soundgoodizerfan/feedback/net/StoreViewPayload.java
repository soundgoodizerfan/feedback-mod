// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.logistics.store.StoreMenu;
import io.github.soundgoodizerfan.feedback.logistics.store.StoreView;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** What an open warehouse screen shows: the whole stock on open, then what changed. */
public record StoreViewPayload(int containerId, StoreView view) implements CustomPacketPayload {

    public static final Type<StoreViewPayload> TYPE = new Type<>(Feedback.id("store_view"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StoreViewPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StoreViewPayload::containerId,
            StoreView.STREAM_CODEC, StoreViewPayload::view,
            StoreViewPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StoreViewPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof StoreMenu menu && menu.containerId == payload.containerId())
                menu.accept(payload.view());
        });
    }
}
