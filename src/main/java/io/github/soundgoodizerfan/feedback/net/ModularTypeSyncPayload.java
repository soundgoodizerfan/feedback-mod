// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.HashMap;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.type.ModularType;
import io.github.soundgoodizerfan.feedback.modular.type.ModularTypes;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Ships the modular type table to the client -- {@link MaterialSyncPayload}'s reasoning. */
public record ModularTypeSyncPayload(Map<ResourceLocation, ModularType> types) implements CustomPacketPayload {

    public static final Type<ModularTypeSyncPayload> TYPE = new Type<>(Feedback.id("modular_type_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ModularTypeSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ModularType.STREAM_CODEC),
                    ModularTypeSyncPayload::types,
                    ModularTypeSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ModularTypeSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ModularTypes.setClient(payload.types()));
    }
}
