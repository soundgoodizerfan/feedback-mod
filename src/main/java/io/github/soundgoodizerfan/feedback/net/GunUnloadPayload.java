// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.weapon.GunAmmo;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The client asked to unload the held gun: its magazine back as a magazine, the chambered round loose. */
public record GunUnloadPayload(boolean offHand) implements CustomPacketPayload {

    public static final Type<GunUnloadPayload> TYPE = new Type<>(Feedback.id("gun_unload"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GunUnloadPayload> STREAM_CODEC =
            net.minecraft.network.codec.ByteBufCodecs.BOOL.map(GunUnloadPayload::new, GunUnloadPayload::offHand).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GunUnloadPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            var stack = payload.offHand() ? player.getOffhandItem() : player.getMainHandItem();
            if (stack.getItem() instanceof GunItem && GunAmmo.count(stack) > 0)
                GunAmmo.eject(player, stack);
        });
    }
}
