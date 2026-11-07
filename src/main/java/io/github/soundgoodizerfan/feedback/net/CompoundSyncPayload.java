// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.HashMap;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.Compound;
import io.github.soundgoodizerfan.feedback.process.CompoundTable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Ships the compound table to the client: the brewing screen previews an ingredient's path from each compound's direction there. */
public record CompoundSyncPayload(Map<String, Compound> compounds) implements CustomPacketPayload {

    public static final Type<CompoundSyncPayload> TYPE = new Type<>(Feedback.id("compound_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CompoundSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.fromCodec(Compound.CODEC)),
            CompoundSyncPayload::compounds,
            CompoundSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CompoundSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> CompoundTable.setClient(payload.compounds()));
    }
}
