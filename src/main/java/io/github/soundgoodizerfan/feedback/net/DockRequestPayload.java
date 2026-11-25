// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.logistics.store.DockMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The dock screen asks: fetch {@code count} of {@code item}, or, with {@code cancel} zero or more, drop that place in the
 * queue.
 */
public record DockRequestPayload(int containerId, ItemStack item, int count, int cancel) implements CustomPacketPayload {

    public static final Type<DockRequestPayload> TYPE = new Type<>(Feedback.id("dock_request"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DockRequestPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DockRequestPayload::containerId,
            ItemStack.OPTIONAL_STREAM_CODEC, DockRequestPayload::item,
            ByteBufCodecs.VAR_INT, DockRequestPayload::count,
            ByteBufCodecs.VAR_INT, DockRequestPayload::cancel,
            DockRequestPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DockRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().containerMenu instanceof DockMenu menu) || menu.containerId != payload.containerId()
                    || !menu.stillValid(context.player()))
                return;
            if (payload.cancel() >= 0)
                menu.cancel(payload.cancel());
            else if (!payload.item().isEmpty())
                menu.request(payload.item(), payload.count());
        });
    }
}
