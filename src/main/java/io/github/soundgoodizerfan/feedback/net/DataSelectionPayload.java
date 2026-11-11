// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.DataSelection;
import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The node the Data Connector now has selected for this player, or none (side -1 for an unsided node; {@code none} true to clear). */
public record DataSelectionPayload(boolean none, BlockPos pos, int side, int voxel) implements CustomPacketPayload {

    public static final Type<DataSelectionPayload> TYPE = new Type<>(Feedback.id("data_selection"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DataSelectionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, DataSelectionPayload::none,
            BlockPos.STREAM_CODEC, DataSelectionPayload::pos,
            ByteBufCodecs.VAR_INT, DataSelectionPayload::side,
            ByteBufCodecs.VAR_INT, p -> p.voxel() + 1,
            (none, pos, side, voxel) -> new DataSelectionPayload(none, pos, side, voxel - 1));

    public static DataSelectionPayload of(DataNodeRef ref) {
        return new DataSelectionPayload(false, ref.pos(), ref.side() == null ? -1 : ref.side().get3DDataValue(), ref.voxel());
    }

    public static DataSelectionPayload cleared() {
        return new DataSelectionPayload(true, BlockPos.ZERO, -1, -1);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DataSelectionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> DataSelection.set(payload.none() ? null
                : new DataNodeRef(payload.pos(), payload.side() < 0 ? null : Direction.from3DDataValue(payload.side()), payload.voxel())));
    }
}
