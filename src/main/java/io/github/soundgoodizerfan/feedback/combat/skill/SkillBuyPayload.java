// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.skill;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The skill screen's buy button. The server checks requirements and cost ({@link Skills#buy}). */
public record SkillBuyPayload(String tree, String node) implements CustomPacketPayload {

    public static final Type<SkillBuyPayload> TYPE = new Type<>(Feedback.id("skill_buy"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SkillBuyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SkillBuyPayload::tree,
            ByteBufCodecs.STRING_UTF8, SkillBuyPayload::node,
            SkillBuyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SkillBuyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Skills.buy((ServerPlayer) context.player(), payload.tree(), payload.node()));
    }
}
