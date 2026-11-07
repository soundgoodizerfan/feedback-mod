// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.HashMap;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * How fast a frame's drive chain and tires turn, so the client can turn them on screen. The spin
 * is computed on the server and never simulated as a body (spec section 5), so without this a
 * driven tube and a dead one look the same -- which is how ten hours of playtesting never saw
 * that no tube was driving (2026-10-16). Rates in rad/s, per tube (a frame may have a drive line per engine); the client integrates the angle.
 */
public record FrameSpinPayload(BlockPos frame, Map<Long, Float> tubes, Map<Long, Float> tubeSu, Map<Integer, Float> hubs)
        implements CustomPacketPayload {

    public static final Type<FrameSpinPayload> TYPE = new Type<>(Feedback.id("frame_spin"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FrameSpinPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FrameSpinPayload::frame,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_LONG, ByteBufCodecs.FLOAT), p -> new HashMap<>(p.tubes()),
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_LONG, ByteBufCodecs.FLOAT), p -> new HashMap<>(p.tubeSu()),
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, ByteBufCodecs.FLOAT), p -> new HashMap<>(p.hubs()),
            FrameSpinPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FrameSpinPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var level = context.player().level(); // the client's: this payload only goes that way
            if (level != null && level.getBlockEntity(payload.frame()) instanceof FrameBlockEntity be) {
                be.spin().set(payload.tubes(), payload.tubeSu(), payload.hubs(), level.getGameTime());
                io.github.soundgoodizerfan.feedback.client.BlockLoops.wheels(be);
            }
        });
    }
}
