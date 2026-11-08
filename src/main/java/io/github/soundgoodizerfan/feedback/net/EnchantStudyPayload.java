// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.EnchantMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The player pressed Study at the Enchanting Table they have open, for an effect the seated part already holds ({@link EnchantMenu#study}). */
public record EnchantStudyPayload(int container, ResourceLocation effect) implements CustomPacketPayload {

    public static final Type<EnchantStudyPayload> TYPE = new Type<>(Feedback.id("enchant_study"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantStudyPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EnchantStudyPayload::container,
            ResourceLocation.STREAM_CODEC, EnchantStudyPayload::effect,
            EnchantStudyPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EnchantStudyPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof EnchantMenu menu
                    && menu.containerId == payload.container())
                menu.study(player, payload.effect());
        });
    }
}
