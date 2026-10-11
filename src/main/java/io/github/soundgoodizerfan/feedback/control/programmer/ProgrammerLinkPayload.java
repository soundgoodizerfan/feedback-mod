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

import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: link card {@code nodeId} to this player's pending Data Connector selection.
 * The one edit that can't happen on the client's local copy, since the selection and the world
 * it resolves against are server-side; the answer comes back as {@link ProgrammerLinkedPayload}.
 * {@code quiet} is the auto-link on placing a card -- nothing pending is not worth a message there.
 */
public record ProgrammerLinkPayload(BlockPos pos, int nodeId, boolean actuator, boolean quiet)
        implements CustomPacketPayload {

    public static final Type<ProgrammerLinkPayload> TYPE = new Type<>(Feedback.id("programmer_link"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProgrammerLinkPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ProgrammerLinkPayload::pos,
                    ByteBufCodecs.VAR_INT, ProgrammerLinkPayload::nodeId,
                    ByteBufCodecs.BOOL, ProgrammerLinkPayload::actuator,
                    ByteBufCodecs.BOOL, ProgrammerLinkPayload::quiet,
                    ProgrammerLinkPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ProgrammerLinkPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Menu check first -- see ProgrammerCommitPayload#handle.
            if (context.player() instanceof ServerPlayer player
                    && player.containerMenu instanceof ProgrammerMenu menu && menu.getPos().equals(payload.pos())
                    && player.level().getBlockEntity(payload.pos()) instanceof ProgrammerBlockEntity programmer)
                programmer.link(payload.nodeId(), payload.actuator(), payload.quiet(), player);
        });
    }
}
