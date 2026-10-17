/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
