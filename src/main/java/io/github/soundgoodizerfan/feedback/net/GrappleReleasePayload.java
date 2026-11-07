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
import io.github.soundgoodizerfan.feedback.combat.tether.GrappleHook;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The player jumped off their grapple lines: the server lets every hook they have out go. The impulse is the client's own (it moves the player). */
public record GrappleReleasePayload() implements CustomPacketPayload {

    public static final Type<GrappleReleasePayload> TYPE = new Type<>(Feedback.id("grapple_release"));

    public static final StreamCodec<ByteBuf, GrappleReleasePayload> STREAM_CODEC = StreamCodec.unit(new GrappleReleasePayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GrappleReleasePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            for (GrappleHook hook : GrappleHook.allOf(player.level(), player))
                hook.release();
        });
    }
}
