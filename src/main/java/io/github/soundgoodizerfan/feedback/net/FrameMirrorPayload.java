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
