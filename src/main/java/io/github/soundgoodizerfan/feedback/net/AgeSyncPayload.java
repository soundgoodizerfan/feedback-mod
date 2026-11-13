// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.Age;
import io.github.soundgoodizerfan.feedback.process.Ages;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Ships the ages so a tooltip can name the age of a treatise volume. */
public record AgeSyncPayload(List<Age> ages) implements CustomPacketPayload {

    public static final Type<AgeSyncPayload> TYPE = new Type<>(Feedback.id("age_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AgeSyncPayload> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(Age.CODEC.listOf()).map(AgeSyncPayload::new, AgeSyncPayload::ages);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AgeSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Ages.get().set(payload.ages()));
    }
}
