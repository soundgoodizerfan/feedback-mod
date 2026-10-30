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

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.potion.BrewingStandMenu;
import io.github.soundgoodizerfan.feedback.potion.BrewingStandBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * What the player is doing at the brewing stand: stirring at this pace (0 to 100) and and whether they chose to cook where the marker is or tip the pot out, pour a ladle over it. Sent a few
 * times a second while the screen is open; the stand is live, so the server applies it as it comes and answers with the map ({@link StandStatePayload}).
 * Only a pace and a choice cross: the marker, the heat and the grade are the server's.
 */
public record StandInputPayload(BlockPos pos, int stir, int action) implements CustomPacketPayload {

    /** What the player chose besides stirring: 0 nothing, cook where the marker is, tip the pot out, pour a ladle over it. */
    public static final int COOK = 1, TIP = 2, LADLE = 3;

    public static final Type<StandInputPayload> TYPE = new Type<>(Feedback.id("stand_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, StandInputPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, StandInputPayload::pos,
            ByteBufCodecs.VAR_INT, StandInputPayload::stir,
            ByteBufCodecs.VAR_INT, StandInputPayload::action,
            StandInputPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StandInputPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !(player.containerMenu instanceof BrewingStandMenu menu)
                    || !menu.pos().equals(payload.pos()) || !(player.level().getBlockEntity(payload.pos()) instanceof BrewingStandBlockEntity stand))
                return;
            stand.stir(player, Math.max(0, Math.min(100, payload.stir())) / 100f);
            if (payload.action() == COOK) {
                String message = stand.brew(player);
                if (message != null)
                    player.displayClientMessage(Component.translatable(message), true);
            } else if (payload.action() == TIP)
                stand.empty();
            else if (payload.action() == LADLE) {
                String message = stand.ladle(player);
                if (message != null)
                    player.displayClientMessage(Component.translatable(message), true);
            }
            PacketDistributor.sendToPlayer(player, stand.state(player));
        });
    }
}
