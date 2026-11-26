// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.bench.PartBenchMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The name typed on the Anvil's Repair tab; blank takes the item's name off. */
public record BenchNamePayload(int containerId, String name) implements CustomPacketPayload {

    public static final Type<BenchNamePayload> TYPE = new Type<>(Feedback.id("bench_name"));

    public static final StreamCodec<RegistryFriendlyByteBuf, BenchNamePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BenchNamePayload::containerId,
            ByteBufCodecs.stringUtf8(256), BenchNamePayload::name,
            BenchNamePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BenchNamePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof PartBenchMenu menu && menu.containerId == payload.containerId()
                    && menu.stillValid(context.player()))
                menu.rename(payload.name());
        });
    }
}
