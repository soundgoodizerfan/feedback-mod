// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.trait.SpearCharge;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The spear's use key: sent on each change and as a heartbeat while held ({@link SpearCharge}). */
public record SpearChargePayload(boolean held) implements CustomPacketPayload {

    public static final Type<SpearChargePayload> TYPE = new Type<>(Feedback.id("spear_charge"));

    public static final StreamCodec<ByteBuf, SpearChargePayload> STREAM_CODEC =
            ByteBufCodecs.BOOL.map(SpearChargePayload::new, SpearChargePayload::held);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SpearChargePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SpearCharge.hold((ServerPlayer) context.player(), payload.held()));
    }
}
