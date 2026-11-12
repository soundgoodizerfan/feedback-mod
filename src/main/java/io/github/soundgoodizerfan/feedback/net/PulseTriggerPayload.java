// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularElectricGun;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A pulse gun's trigger went down or came up. A flywheel's spin and a hold charge are built from how long the trigger is held, so the
 * server is told the two edges (the client applies the same edit to its own stack at once, so the two agree on the clock).
 */
public record PulseTriggerPayload(boolean down, boolean offHand) implements CustomPacketPayload {

    public static final Type<PulseTriggerPayload> TYPE = new Type<>(Feedback.id("pulse_trigger"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PulseTriggerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, PulseTriggerPayload::down,
            ByteBufCodecs.BOOL, PulseTriggerPayload::offHand,
            PulseTriggerPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PulseTriggerPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            var stack = payload.offHand() ? player.getOffhandItem() : player.getMainHandItem();
            if (stack.getItem() instanceof ModularElectricGun)
                ModularElectricGun.setHeld(stack, payload.down(), player.level().getGameTime());
        });
    }
}
