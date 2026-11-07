// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.ClientDeformations;
import io.github.soundgoodizerfan.feedback.process.Deformation;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Ships the deformation table to the client so a recipe browser can print it.
 *
 * <h2>Why a packet rather than JEI's own hooks</h2>
 * Both were available and the packet wins on honesty. The table is datapack data, so the client
 * genuinely does not have it -- that is a missing-data problem, not a JEI problem, and solving it
 * in JEI would mean any other display we ever write has to solve it again. Sending it once on
 * datapack sync leaves a plain list any consumer can read, and JEI becomes an optional reader of
 * it rather than the owner of it.
 * <p>
 * It also keeps the deletion story simple: if JEI is not installed, this packet still arrives and
 * costs three entries of memory.
 */
public record DeformationSyncPayload(List<Deformation> entries) implements CustomPacketPayload {

    public static final Type<DeformationSyncPayload> TYPE = new Type<>(Feedback.id("deformation_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DeformationSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    Deformation.STREAM_CODEC.apply(ByteBufCodecs.list()), DeformationSyncPayload::entries,
                    DeformationSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DeformationSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientDeformations.set(payload.entries()));
    }
}
