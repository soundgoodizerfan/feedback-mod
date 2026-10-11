/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
 * Client -> server: the whole edited graph and layout from a {@link ProgrammerScreen}, replacing
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
                    && context.player().level().getBlockEntity(payload.pos()) instanceof ProgrammerBlockEntity programmer)
                programmer.commit(payload.graph(), payload.positions(), payload.print(), context.player());
        });
    }
}
