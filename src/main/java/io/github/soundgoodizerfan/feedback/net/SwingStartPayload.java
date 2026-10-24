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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A player began a melee swing with an upswing (client/UpswingSwing): the clip, how long the whole swing
 * takes, where in it the strike falls, and the sound for that moment. The server only relays it to the
 * players who can see the swinger, since the blow itself arrives later as an ordinary attack; it checks
 * nothing but that the sender may act, because the swing carries no damage.
 */
public record SwingStartPayload(String clip, int hand, float length, float upswing, String sound)
        implements CustomPacketPayload {

    public static final Type<SwingStartPayload> TYPE = new Type<>(Feedback.id("swing_start"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwingStartPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SwingStartPayload::clip,
            ByteBufCodecs.VAR_INT, SwingStartPayload::hand,
            ByteBufCodecs.FLOAT, SwingStartPayload::length,
            ByteBufCodecs.FLOAT, SwingStartPayload::upswing,
            ByteBufCodecs.STRING_UTF8, SwingStartPayload::sound,
            SwingStartPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SwingStartPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator())
                return;
            PacketDistributor.sendToPlayersTrackingEntity(player, new SwingPlayPayload(player.getId(), payload.clip(),
                    payload.hand(), Math.clamp(payload.length(), 1f, 100f), Math.clamp(payload.upswing(), 0.05f, 0.95f),
                    payload.sound()));
        });
    }
}
