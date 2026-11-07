// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.HashMap;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Ships the per-dimension ambient table so the client's workpiece readout cools towards the same
 * floor the server does. Unlike the process tables this one feeds a computation -- a tooltip's
 * current temperature -- though still never the simulation.
 */
public record AmbientSyncPayload(Map<ResourceLocation, Float> entries) implements CustomPacketPayload {

    public static final Type<AmbientSyncPayload> TYPE = new Type<>(Feedback.id("ambient_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AmbientSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.FLOAT),
                    AmbientSyncPayload::entries,
                    AmbientSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AmbientSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> AmbientTable.get().set(payload.entries()));
    }
}
