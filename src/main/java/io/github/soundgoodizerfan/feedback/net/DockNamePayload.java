// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.logistics.store.DockMenu;
import io.github.soundgoodizerfan.feedback.logistics.store.StoreTuning;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The name typed on an open dock screen; blank takes the name off. */
public record DockNamePayload(int containerId, String name) implements CustomPacketPayload {

    public static final Type<DockNamePayload> TYPE = new Type<>(Feedback.id("dock_name"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DockNamePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DockNamePayload::containerId,
            ByteBufCodecs.stringUtf8(StoreTuning.DOCK_NAME_MAX), DockNamePayload::name,
            DockNamePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DockNamePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof DockMenu menu && menu.containerId == payload.containerId()
                    && menu.stillValid(context.player()))
                menu.rename(payload.name());
        });
    }
}
