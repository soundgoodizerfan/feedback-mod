// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.process.ProcessTuning;
import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.item.SolderingIronItem;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A hand-soldering drag, once, at its end: for each pad the tip touched, its index,
 * the ticks it sat there and the furthest it strayed off the trace in hundredths of a pad pitch,
 * and whether the button was still down as the screen closed. Carries no grade: the server replays
 * the samples through {@code Soldering.Pass} against the iron's real heat and refuses any whose
 * counts cannot have happened ({@link SolderingIronItem#finishDrag}).
 */
public record SolderFinishPayload(List<Integer> pads, List<Integer> dwell, List<Integer> offset, boolean held) implements CustomPacketPayload {

    public static final Type<SolderFinishPayload> TYPE = new Type<>(Feedback.id("solder_finish"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SolderFinishPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(ProcessTuning.SOLDER_MAX_PADS)), SolderFinishPayload::pads,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(ProcessTuning.SOLDER_MAX_PADS)), SolderFinishPayload::dwell,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(ProcessTuning.SOLDER_MAX_PADS)), SolderFinishPayload::offset,
            ByteBufCodecs.BOOL, SolderFinishPayload::held,
            SolderFinishPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SolderFinishPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player)
                SolderingIronItem.finishDrag(player, payload);
        });
    }
}
