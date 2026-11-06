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
import io.github.soundgoodizerfan.feedback.modular.Shingling;
import io.github.soundgoodizerfan.feedback.modular.ToolTableMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The inputs of a shingling, as samples: tick from the start, kind (0 light, 1 heavy, 2 turn) and where on the billet face in thousandths.
 * The menu replays them through {@link Shingling} against the bloom's own heat and the hammer's own blows ({@link ToolTableMenu#finishShingle}).
 */
public record ShingleFinishPayload(int container, List<Integer> ticks, List<Integer> kinds, List<Integer> xs, List<Integer> ys) implements CustomPacketPayload {

    public static final Type<ShingleFinishPayload> TYPE = new Type<>(Feedback.id("shingle_finish"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ShingleFinishPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ShingleFinishPayload::container,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(256)), ShingleFinishPayload::ticks,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(256)), ShingleFinishPayload::kinds,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(256)), ShingleFinishPayload::xs,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(256)), ShingleFinishPayload::ys,
            ShingleFinishPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ShingleFinishPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof ToolTableMenu menu
                    && menu.containerId == payload.container())
                menu.finishShingle(player, Shingling.events(payload.ticks(), payload.kinds(), payload.xs(), payload.ys()));
        });
    }
}
