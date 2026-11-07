// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.SoundingOverlay;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A sounding pulse's finds, to the one player who sent it: ore positions to outline for {@code ticks}. */
public record SoundingPayload(List<BlockPos> found, int ticks) implements CustomPacketPayload {

    public static final Type<SoundingPayload> TYPE = new Type<>(Feedback.id("sounding"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SoundingPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), SoundingPayload::found,
            ByteBufCodecs.VAR_INT, SoundingPayload::ticks,
            SoundingPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SoundingPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SoundingOverlay.show(payload.found(), payload.ticks()));
    }
}
