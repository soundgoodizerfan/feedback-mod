// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.creative.CreativeSource;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A creative source's settings screen, sent to the server for the block the player has open. */
public record CreativeSourcePayload(BlockPos pos, boolean on, List<Float> values) implements CustomPacketPayload {

    public static final Type<CreativeSourcePayload> TYPE = new Type<>(Feedback.id("creative_source"));

    public static final StreamCodec<ByteBuf, CreativeSourcePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CreativeSourcePayload::pos,
            ByteBufCodecs.BOOL, CreativeSourcePayload::on,
            ByteBufCodecs.FLOAT.apply(ByteBufCodecs.list(16)), CreativeSourcePayload::values,
            CreativeSourcePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CreativeSourcePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (!player.canUseGameMasterBlocks() || player.distanceToSqr(payload.pos().getCenter()) > 64)
                return;
            if (player.level().getBlockEntity(payload.pos()) instanceof CreativeSource source
                    && payload.values().size() == source.params().size()) {
                float[] v = new float[payload.values().size()];
                for (int i = 0; i < v.length; i++)
                    v[i] = payload.values().get(i);
                source.configure(payload.on(), v);
            }
        });
    }
}
