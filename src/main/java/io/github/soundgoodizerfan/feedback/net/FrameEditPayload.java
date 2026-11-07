// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.vehicle.frame.FrameEdit;
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A gizmo drag or click: move (or extrude) the held selection {@code steps} 1/16 cells along {@code axis}. */
public record FrameEditPayload(BlockPos frame, int axis, int steps, boolean extrude) implements CustomPacketPayload {

    public static final Type<FrameEditPayload> TYPE = new Type<>(Feedback.id("frame_edit"));

    public static final StreamCodec<ByteBuf, FrameEditPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, FrameEditPayload::frame,
            ByteBufCodecs.VAR_INT, FrameEditPayload::axis,
            ByteBufCodecs.VAR_INT, FrameEditPayload::steps,
            ByteBufCodecs.BOOL, FrameEditPayload::extrude,
            FrameEditPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FrameEditPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            var stack = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!(stack.getItem() instanceof WelderItem))
                return;
            WelderItem.Aim aim = WelderItem.aim(player.level(), player);
            if (aim == null || !aim.be().getBlockPos().equals(payload.frame()))
                return;
            FrameEdit.apply(player, stack, aim.be(), payload.axis(), Math.clamp(payload.steps(), -256, 256), payload.extrude());
        });
    }
}
