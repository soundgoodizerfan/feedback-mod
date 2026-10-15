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
