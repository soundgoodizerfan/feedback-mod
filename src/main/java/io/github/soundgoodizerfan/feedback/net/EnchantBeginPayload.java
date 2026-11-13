// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.enchant.EnchantMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The player pressed Begin at the Enchanting Table they have open, for this effect ({@link EnchantMenu#begin}). */
public record EnchantBeginPayload(int container, ResourceLocation effect) implements CustomPacketPayload {

    public static final Type<EnchantBeginPayload> TYPE = new Type<>(Feedback.id("enchant_begin"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantBeginPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EnchantBeginPayload::container,
            ResourceLocation.STREAM_CODEC, EnchantBeginPayload::effect,
            EnchantBeginPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EnchantBeginPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof EnchantMenu menu
                    && menu.containerId == payload.container())
                menu.begin(player, payload.effect());
        });
    }
}
