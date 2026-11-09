// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.computer;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: something an app keeps on the block. {@code NOTES} carries the Notes text, {@code SNAKE} and {@code MINES} a finished
 * game's score. Only honoured while that player has that Computer's desktop open; the block entity clamps whatever arrives.
 */
public record ComputerDataPayload(BlockPos pos, int kind, int value, String text) implements CustomPacketPayload {

    public static final int NOTES = 0, SNAKE = 1, MINES = 2;

    public static final Type<ComputerDataPayload> TYPE = new Type<>(Feedback.id("computer_data"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ComputerDataPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ComputerDataPayload::pos,
            ByteBufCodecs.VAR_INT, ComputerDataPayload::kind,
            ByteBufCodecs.VAR_INT, ComputerDataPayload::value,
            ByteBufCodecs.stringUtf8(ComputerBlockEntity.NOTES_MAX), ComputerDataPayload::text,
            ComputerDataPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ComputerDataPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().containerMenu instanceof ComputerMenu menu) || !menu.getPos().equals(payload.pos()) || menu.getComputer() == null)
                return;
            switch (payload.kind()) {
                case NOTES -> menu.getComputer().setNotes(payload.text());
                case SNAKE -> menu.getComputer().score(0, payload.value());
                case MINES -> menu.getComputer().score(1, payload.value());
                default -> { }
            }
        });
    }
}
