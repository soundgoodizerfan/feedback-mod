// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The pump key was pressed on a held gun: the server works its pump charge. */
public record GunPumpPayload() implements CustomPacketPayload {

    public static final Type<GunPumpPayload> TYPE = new Type<>(Feedback.id("gun_pump"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GunPumpPayload> STREAM_CODEC = StreamCodec.unit(new GunPumpPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GunPumpPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            for (InteractionHand hand : InteractionHand.values()) {
                var stack = player.getItemInHand(hand);
                if (stack.getItem() instanceof GunItem gun && player.level() instanceof ServerLevel level) {
                    gun.mechanism().pumpKey(level, player, stack);
                    return;
                }
            }
        });
    }
}
