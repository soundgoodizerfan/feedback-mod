// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.ClientPayloads;
import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * An oscilloscope's last {@link ElectricMachineTuning#SCOPE_HISTORY} samples for the phosphor screen it drives, oldest first, each as a
 * share of full scale (0 to 1). Sent to players tracking the chunk, and only when the row changed (or as a slow keepalive); the screen
 * never saves it.
 */
public record ScopeTracePayload(BlockPos screen, float[] samples) implements CustomPacketPayload {

    public static final Type<ScopeTracePayload> TYPE = new Type<>(Feedback.id("scope_trace"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ScopeTracePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.screen);
                for (int i = 0; i < ElectricMachineTuning.SCOPE_HISTORY; i++)
                    buf.writeFloat(p.samples[i]);
            },
            buf -> {
                BlockPos pos = buf.readBlockPos();
                float[] samples = new float[ElectricMachineTuning.SCOPE_HISTORY];
                for (int i = 0; i < samples.length; i++)
                    samples[i] = buf.readFloat();
                return new ScopeTracePayload(pos, samples);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ScopeTracePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientPayloads.scopeTrace(payload));
    }
}
