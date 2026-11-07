// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.programmer;

import io.github.soundgoodizerfan.feedback.client.ClientPayloads;
import java.util.Optional;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server -> client: a {@link ProgrammerLinkPayload} succeeded; the open screen patches card {@code nodeId} in its local copy. */
public record ProgrammerLinkedPayload(BlockPos pos, int nodeId, DataNodeRef target, Optional<Quantity> quantity,
                                      int channel, int token)
        implements CustomPacketPayload {

    public static final Type<ProgrammerLinkedPayload> TYPE = new Type<>(Feedback.id("programmer_linked"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProgrammerLinkedPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ProgrammerLinkedPayload::pos,
                    ByteBufCodecs.VAR_INT, ProgrammerLinkedPayload::nodeId,
                    DataNodeRef.STREAM_CODEC.cast(), ProgrammerLinkedPayload::target,
                    ByteBufCodecs.optional(Quantity.STREAM_CODEC).cast(), ProgrammerLinkedPayload::quantity,
                    ByteBufCodecs.VAR_INT, ProgrammerLinkedPayload::channel,
                    ByteBufCodecs.VAR_INT, ProgrammerLinkedPayload::token,
                    ProgrammerLinkedPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Only ever runs on the client; the work is in {@code client/}, so no screen class is named here. */
    public static void handle(ProgrammerLinkedPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPayloads.programmerLinked(payload));
    }
}
