// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The frame screen's mirror planes and lock, sent to the server for the frame the player is working on. */
public record FrameMirrorPayload(BlockPos frame, int mirror, boolean locked) implements CustomPacketPayload {

    public static final Type<FrameMirrorPayload> TYPE = new Type<>(Feedback.id("frame_mirror"));

    public static final StreamCodec<ByteBuf, FrameMirrorPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FrameMirrorPayload::frame,
            ByteBufCodecs.VAR_INT, FrameMirrorPayload::mirror,
            ByteBufCodecs.BOOL, FrameMirrorPayload::locked,
            FrameMirrorPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FrameMirrorPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            WelderItem.Aim aim = WelderItem.aim(player.level(), player);
            // Only the frame the player is at: a packet naming any other is ignored.
            if (aim == null || !aim.be().getBlockPos().equals(payload.frame()))
                return;
            FrameBlockEntity be = aim.be();
            be.frame().setMirror(payload.mirror());
            be.frame().setLocked(payload.locked());
            be.changed();
        });
    }
}
