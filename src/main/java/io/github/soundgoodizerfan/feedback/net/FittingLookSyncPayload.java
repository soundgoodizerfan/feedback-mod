// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.fitting.FittingLooks;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Ships the fitting look tables ({@link FittingLooks}: what each kind of fitting looks like and where each holder carries them) so the
 * client's {@code FittingRenderer} can draw what the server has fitted. Display only: nothing on the client simulates with it.
 */
public record FittingLookSyncPayload(Map<ResourceLocation, FittingLooks.Look> models,
                                     Map<ResourceLocation, FittingLooks.HolderAnchors> anchors) implements CustomPacketPayload {

    public static final Type<FittingLookSyncPayload> TYPE = new Type<>(Feedback.id("fitting_look_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FittingLookSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodecWithRegistries(FittingLooks.MODELS_CODEC), FittingLookSyncPayload::models,
            ByteBufCodecs.fromCodecWithRegistries(FittingLooks.ANCHORS_CODEC), FittingLookSyncPayload::anchors,
            FittingLookSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FittingLookSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> FittingLooks.set(payload.models(), payload.anchors()));
    }
}
