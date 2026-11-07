// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.HashMap;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.Attunements;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Ships the attunement effects to the client, as {@link GunOptionSyncPayload} does: the gauge and the bake run there too. */
public record AttunementSyncPayload(Map<ResourceLocation, Attunements.Effect> effects) implements CustomPacketPayload {

    public static final Type<AttunementSyncPayload> TYPE = new Type<>(Feedback.id("attunement_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AttunementSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.fromCodec(Attunements.Effect.CODEC)),
            AttunementSyncPayload::effects,
            AttunementSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AttunementSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Attunements.setClient(payload.effects()));
    }
}
