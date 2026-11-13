// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.hud.SkillGainHud;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A kill that paid skill points, to the killer only: one wisp in the tree's colour {@code rgb} drifts from the fallen at (x, y, z) to them, if their client config allows it. */
public record SkillWispPayload(int rgb, double x, double y, double z) implements CustomPacketPayload {

    public static final Type<SkillWispPayload> TYPE = new Type<>(Feedback.id("skill_wisp"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SkillWispPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SkillWispPayload::rgb,
            ByteBufCodecs.DOUBLE, SkillWispPayload::x,
            ByteBufCodecs.DOUBLE, SkillWispPayload::y,
            ByteBufCodecs.DOUBLE, SkillWispPayload::z,
            SkillWispPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SkillWispPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SkillGainHud.wisp(payload.rgb(), payload.x(), payload.y(), payload.z()));
    }
}
