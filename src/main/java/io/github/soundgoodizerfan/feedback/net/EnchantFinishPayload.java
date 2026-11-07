// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;
import io.github.soundgoodizerfan.feedback.modular.EnchantMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The pen the player held, one packed sample per tick ({@link io.github.soundgoodizerfan.feedback.modular.Sigil#pack}: pad x,
 * pad y 0..255, pen-down bit), for the Enchanting Table they have open. The menu replays it through
 * {@code Sigil.run} from the start state it noted at Begin and judges that; the client's own result is never sent, so a
 * fabricated result is only ever a fabricated pen path ({@link EnchantMenu#finish}). It does
 * send the heat it started from: Begin crosses the network and the part cooled meanwhile, so replaying from the server's
 * own figure judged the screen's result differently near a grade's edge. The menu takes it when it is within a few ticks'
 * cooling of its own.
 */
public record EnchantFinishPayload(int container, ResourceLocation effect, float start, List<Integer> samples) implements CustomPacketPayload {

    public static final Type<EnchantFinishPayload> TYPE = new Type<>(Feedback.id("enchant_finish"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantFinishPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EnchantFinishPayload::container,
            ResourceLocation.STREAM_CODEC, EnchantFinishPayload::effect,
            ByteBufCodecs.FLOAT, EnchantFinishPayload::start,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(ModularTuning.ENCHANT_MAX_TICKS)), EnchantFinishPayload::samples,
            EnchantFinishPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EnchantFinishPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof EnchantMenu menu
                    && menu.containerId == payload.container())
                menu.finish(player, payload.effect(), payload.start(), payload.samples());
        });
    }
}
