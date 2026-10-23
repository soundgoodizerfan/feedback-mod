/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
 * The field the player held, one sample per tick (-100..100, {@code EnchantGame#field}), for the Enchanting Table they
 * have open. The menu replays it from the start state it noted at Begin and judges that; the client's own
 * polarization is never sent, so a fabricated result is only ever a fabricated field ({@link EnchantMenu#finish}).
 */
public record EnchantFinishPayload(int container, ResourceLocation effect, List<Integer> samples) implements CustomPacketPayload {

    public static final Type<EnchantFinishPayload> TYPE = new Type<>(Feedback.id("enchant_finish"));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantFinishPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, EnchantFinishPayload::container,
            ResourceLocation.STREAM_CODEC, EnchantFinishPayload::effect,
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
                menu.finish(player, payload.effect(), payload.samples());
        });
    }
}
