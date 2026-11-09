// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.computer;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client -> server: the Settings window changed how this Computer looks. Only honoured while the player has that Computer's desktop open. */
public record ComputerSettingsPayload(BlockPos pos, ComputerSettings settings) implements CustomPacketPayload {

    public static final Type<ComputerSettingsPayload> TYPE = new Type<>(Feedback.id("computer_settings"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ComputerSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ComputerSettingsPayload::pos,
            ComputerSettings.STREAM_CODEC, ComputerSettingsPayload::settings,
            ComputerSettingsPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ComputerSettingsPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof ComputerMenu menu && menu.getPos().equals(payload.pos()) && menu.getComputer() != null)
                menu.getComputer().setSettings(payload.settings());
        });
    }
}
