// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.computer;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: a button on the Computer's desktop. Only honoured while that player has that Computer's desktop open. */
public record ComputerActionPayload(BlockPos pos, int action, int drive) implements CustomPacketPayload {

    public static final int RUN = 0, STOP = 1, EDIT = 2, EJECT = 3, CLOSE_EDITOR = 4, COPY = 5;

    public static final Type<ComputerActionPayload> TYPE = new Type<>(Feedback.id("computer_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ComputerActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ComputerActionPayload::pos,
            ByteBufCodecs.VAR_INT, ComputerActionPayload::action,
            ByteBufCodecs.VAR_INT, ComputerActionPayload::drive,
            ComputerActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ComputerActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof ComputerMenu menu)
                    || !menu.getPos().equals(payload.pos()) || menu.getComputer() == null)
                return;
            ComputerBlockEntity computer = menu.getComputer();
            switch (payload.action()) {
                case RUN -> computer.run(payload.drive(), player);
                case STOP -> computer.stop(player);
                case EDIT -> computer.edit(payload.drive(), player);
                case EJECT -> computer.eject(payload.drive(), player);
                case CLOSE_EDITOR -> computer.closeEditor(player);
                case COPY -> computer.copy(payload.drive(), player);
                default -> { }
            }
        });
    }
}
