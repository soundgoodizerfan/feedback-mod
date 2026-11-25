// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.logistics.store.DockMenu;
import io.github.soundgoodizerfan.feedback.logistics.store.DockView;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** What the open dock screen shows, sent on open and on change. */
public record DockViewPayload(int containerId, DockView view) implements CustomPacketPayload {

    public static final Type<DockViewPayload> TYPE = new Type<>(Feedback.id("dock_view"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DockViewPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DockViewPayload::containerId,
            DockView.STREAM_CODEC, DockViewPayload::view,
            DockViewPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DockViewPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof DockMenu menu && menu.containerId == payload.containerId())
                menu.accept(payload.view());
        });
    }
}
