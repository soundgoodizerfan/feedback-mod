// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.ClientMaterials;
import io.github.soundgoodizerfan.feedback.process.Material;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Ships the material table to the client -- {@link DeformationSyncPayload}'s reasoning. First
 * wanted by the Engine Stand, whose screen shows what a design will do as it is edited, and that
 * is figures read off the chosen material.
 */
public record MaterialSyncPayload(Map<ResourceLocation, Material> materials) implements CustomPacketPayload {

    public static final Type<MaterialSyncPayload> TYPE = new Type<>(Feedback.id("material_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MaterialSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, ByteBufCodecs.fromCodec(Material.CODEC)),
                    MaterialSyncPayload::materials,
                    MaterialSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MaterialSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientMaterials.set(payload.materials()));
    }
}
