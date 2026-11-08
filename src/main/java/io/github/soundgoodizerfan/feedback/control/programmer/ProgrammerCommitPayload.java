// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.programmer;

import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: the whole edited graph and layout from a {@code ProgrammerScreen}, replacing
 * the Programmer's working copy in one go -- the screen edits a local copy (which is what makes
 * undo and copy/paste cheap) and sends it on Print and on close. {@code print} burns the card
 * straight after, so Print never races a separate commit.
 */
public record ProgrammerCommitPayload(BlockPos pos, ProgramGraph graph, Map<Integer, NodePos> positions,
                                      boolean print) implements CustomPacketPayload {

    public static final Type<ProgrammerCommitPayload> TYPE = new Type<>(Feedback.id("programmer_commit"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProgrammerCommitPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ProgrammerCommitPayload::pos,
                    ProgramGraph.STREAM_CODEC.cast(), ProgrammerCommitPayload::graph,
                    NodePos.MAP_STREAM_CODEC.cast(), ProgrammerCommitPayload::positions,
                    ByteBufCodecs.BOOL, ProgrammerCommitPayload::print,
                    ProgrammerCommitPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ProgrammerCommitPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Menu check first: an arbitrary pos from the client must never reach getBlockEntity,
            // which would load that chunk.
            if (context.player().containerMenu instanceof ProgrammerMenu menu && menu.getPos().equals(payload.pos())
                    && menu.getProgrammer() != null)
                menu.getProgrammer().commit(payload.graph(), payload.positions(), payload.print(), context.player());
        });
    }
}
