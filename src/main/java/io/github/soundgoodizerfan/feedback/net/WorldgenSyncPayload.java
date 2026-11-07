// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.geology.Basin;
import io.github.soundgoodizerfan.feedback.process.ClientTable;
import io.github.soundgoodizerfan.feedback.worldgen.OreVein;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Ships the ore-vein and basin tables so a recipe browser can list where things occur -- {@link DipSyncPayload}'s reasoning. */
public record WorldgenSyncPayload(List<OreVein> veins, List<Basin> basins) implements CustomPacketPayload {

    public static final Type<WorldgenSyncPayload> TYPE = new Type<>(Feedback.id("worldgen_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WorldgenSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodecWithRegistries(OreVein.CODEC.listOf()), WorldgenSyncPayload::veins,
            ByteBufCodecs.fromCodecWithRegistries(Basin.CODEC.listOf()), WorldgenSyncPayload::basins,
            WorldgenSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WorldgenSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientTable.ORE_VEINS.set(payload.veins());
            ClientTable.BASINS.set(payload.basins());
        });
    }
}
