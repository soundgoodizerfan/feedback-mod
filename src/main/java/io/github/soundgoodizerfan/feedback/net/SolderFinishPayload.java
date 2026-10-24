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
package io.github.soundgoodizerfan.feedback.net;

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.item.SolderingIronItem;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A hand-soldering drag, once, at its end (mechanics 6.22): for each pad the tip touched, its index,
 * the ticks it sat there and the furthest it strayed off the trace in hundredths of a pad pitch,
 * and whether the button was still down as the screen closed. Carries no grade: the server replays
 * the samples through {@code Soldering.Pass} against the iron's real heat and refuses any whose
 * counts cannot have happened ({@link SolderingIronItem#finishDrag}).
 */
public record SolderFinishPayload(List<Integer> pads, List<Integer> dwell, List<Integer> offset, boolean held) implements CustomPacketPayload {

    public static final Type<SolderFinishPayload> TYPE = new Type<>(Feedback.id("solder_finish"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SolderFinishPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(FTuning.SOLDER_MAX_PADS)), SolderFinishPayload::pads,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(FTuning.SOLDER_MAX_PADS)), SolderFinishPayload::dwell,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(FTuning.SOLDER_MAX_PADS)), SolderFinishPayload::offset,
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
