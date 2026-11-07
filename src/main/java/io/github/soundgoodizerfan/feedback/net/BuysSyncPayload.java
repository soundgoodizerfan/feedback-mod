// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.ClientTable;
import io.github.soundgoodizerfan.feedback.village.Want;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Ships the table of what villagers buy so a recipe browser can print each pool. */
public record BuysSyncPayload(List<Want> entries) implements CustomPacketPayload {

    public static final Type<BuysSyncPayload> TYPE = new Type<>(Feedback.id("buys_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BuysSyncPayload> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(Want.CODEC.listOf()).map(BuysSyncPayload::new, BuysSyncPayload::entries);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BuysSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientTable.BUYS.set(payload.entries()));
    }
}
