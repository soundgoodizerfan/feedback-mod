// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.ClientRemovals;
import io.github.soundgoodizerfan.feedback.process.Removal;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Ships the removal table to the client -- {@link DeformationSyncPayload}'s own reasoning. */
public record RemovalSyncPayload(List<Removal> entries) implements CustomPacketPayload {

    public static final Type<RemovalSyncPayload> TYPE = new Type<>(Feedback.id("removal_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemovalSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    Removal.STREAM_CODEC.apply(ByteBufCodecs.list()), RemovalSyncPayload::entries,
                    RemovalSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RemovalSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientRemovals.set(payload.entries()));
    }
}
