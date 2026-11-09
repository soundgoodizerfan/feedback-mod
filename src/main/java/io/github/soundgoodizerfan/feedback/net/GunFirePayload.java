// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunShots;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The client fired. It has already played the flash, sound and kick (spec 7: prediction); this
 * asks the server to validate and resolve. The aim is the view at the moment of the press; {@code offHand} says which gun pulled.
 */
public record GunFirePayload(float yaw, float pitch, boolean aimed, boolean offHand) implements CustomPacketPayload {

    public static final Type<GunFirePayload> TYPE = new Type<>(Feedback.id("gun_fire"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GunFirePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, GunFirePayload::yaw,
            ByteBufCodecs.FLOAT, GunFirePayload::pitch,
            ByteBufCodecs.BOOL, GunFirePayload::aimed,
            ByteBufCodecs.BOOL, GunFirePayload::offHand,
            GunFirePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GunFirePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (!(player.level() instanceof ServerLevel level) || player.isSpectator()
                    || !Float.isFinite(payload.yaw()) || !Float.isFinite(payload.pitch()))
                return;
            var hand = payload.offHand() ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND;
            var stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof GunItem) || (payload.offHand() && !GunItem.offHandFires(player)))
                return;
            GunShots.triggerFromClient(level, player, stack, payload.yaw(), payload.pitch(), System.currentTimeMillis(),
                    payload.aimed() && !payload.offHand(), hand);
        });
    }
}
