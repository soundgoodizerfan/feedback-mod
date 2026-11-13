// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.combat.SpearClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** To players tracking a charging wielder: their stage, 0 none, 1 engaged, 2 tired, 3 disengaged. */
public record SpearStagePayload(int entity, int stage) implements CustomPacketPayload {

    public static final Type<SpearStagePayload> TYPE = new Type<>(Feedback.id("spear_stage"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SpearStagePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SpearStagePayload::entity,
            ByteBufCodecs.VAR_INT, SpearStagePayload::stage,
            SpearStagePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SpearStagePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SpearClient.setStage(payload.entity(), payload.stage()));
    }
}
