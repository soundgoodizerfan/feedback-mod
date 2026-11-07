// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.ClientThermalProcesses;
import io.github.soundgoodizerfan.feedback.process.ThermalProcess;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Ships the thermal process table to the client so a recipe browser can print it.
 * <p>
 * A second packet rather than a generalised one. The two tables are different types with
 * different lifetimes and folding them into one payload would mean a wrapper whose only purpose
 * is that both happen to be sent at the same moment. See {@link DeformationSyncPayload} for the
 * argument about why either is sent at all.
 */
public record ThermalProcessSyncPayload(List<ThermalProcess> entries) implements CustomPacketPayload {

    public static final Type<ThermalProcessSyncPayload> TYPE = new Type<>(Feedback.id("thermal_process_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ThermalProcessSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ThermalProcess.STREAM_CODEC.apply(ByteBufCodecs.list()), ThermalProcessSyncPayload::entries,
                    ThermalProcessSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ThermalProcessSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientThermalProcesses.set(payload.entries()));
    }
}
