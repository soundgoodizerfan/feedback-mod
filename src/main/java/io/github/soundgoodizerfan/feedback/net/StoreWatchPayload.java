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

/** A dock screen sets what program channel {@code channel} (1 to 4) watches; an empty {@code item} clears it. */
public record StoreWatchPayload(int containerId, int channel, ItemStack item) implements CustomPacketPayload {

    public static final Type<StoreWatchPayload> TYPE = new Type<>(Feedback.id("store_watch"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StoreWatchPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StoreWatchPayload::containerId,
            ByteBufCodecs.VAR_INT, StoreWatchPayload::channel,
            ItemStack.OPTIONAL_STREAM_CODEC, StoreWatchPayload::item,
            StoreWatchPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StoreWatchPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof DockMenu menu && menu.containerId == payload.containerId()
                    && menu.stillValid(context.player()))
                menu.watch(payload.channel(), payload.item());
        });
    }
}
