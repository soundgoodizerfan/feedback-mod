// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.ClientTable;
import io.github.soundgoodizerfan.feedback.process.Mixture;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Ships the mixture table so a recipe browser can print it -- {@link CastingSyncPayload}'s reasoning. */
public record MixtureSyncPayload(List<Mixture> entries) implements CustomPacketPayload {

    public static final Type<MixtureSyncPayload> TYPE = new Type<>(Feedback.id("mixture_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MixtureSyncPayload> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(Mixture.CODEC.listOf())
                    .map(MixtureSyncPayload::new, MixtureSyncPayload::entries);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MixtureSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientTable.MIXTURES.set(payload.entries()));
    }
}
