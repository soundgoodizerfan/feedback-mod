// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.GunItem;
import io.github.soundgoodizerfan.feedback.weapon.GunSpec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The client asked to switch the held gun's fire mode, round the modes its action has. */
public record GunModePayload(boolean offHand) implements CustomPacketPayload {

    public static final Type<GunModePayload> TYPE = new Type<>(Feedback.id("gun_mode"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GunModePayload> STREAM_CODEC =
            net.minecraft.network.codec.ByteBufCodecs.BOOL.map(GunModePayload::new, GunModePayload::offHand).cast();

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GunModePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var stack = payload.offHand() ? context.player().getOffhandItem() : context.player().getMainHandItem();
            var design = GunSpec.of(stack).orElse(null);
            if (stack.getItem() instanceof GunItem && design != null)
                stack.set(FWeapons.FIRE_MODE.get(), design.layout().action().next(GunItem.mode(stack)));
        });
    }
}
