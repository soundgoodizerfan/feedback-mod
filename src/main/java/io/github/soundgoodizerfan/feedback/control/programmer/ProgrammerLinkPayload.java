// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
public record ProgrammerLinkPayload(BlockPos pos, int nodeId, boolean actuator, boolean quiet, int token)
        implements CustomPacketPayload {

    public static final Type<ProgrammerLinkPayload> TYPE = new Type<>(Feedback.id("programmer_link"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProgrammerLinkPayload> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ProgrammerLinkPayload::pos,
                    ByteBufCodecs.VAR_INT, ProgrammerLinkPayload::nodeId,
                    ByteBufCodecs.BOOL, ProgrammerLinkPayload::actuator,
                    ByteBufCodecs.BOOL, ProgrammerLinkPayload::quiet,
                    ByteBufCodecs.VAR_INT, ProgrammerLinkPayload::token,
                    ProgrammerLinkPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ProgrammerLinkPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Menu check first -- see ProgrammerCommitPayload#handle.
            if (context.player() instanceof ServerPlayer player
                    && player.containerMenu instanceof ProgrammerAccess menu && menu.getPos().equals(payload.pos())
                    && menu.getProgrammer() != null)
                menu.getProgrammer().link(payload.nodeId(), payload.actuator(), payload.quiet(), payload.token(), player);
        });
    }
}
