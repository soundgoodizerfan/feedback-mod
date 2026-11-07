// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
