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
import io.github.soundgoodizerfan.feedback.client.GunClient;
import io.github.soundgoodizerfan.feedback.weapon.GunFigures;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A shot the server fired for a player (a cook-off): the client kicks the gun and the camera as it does for a shot of its own,
 * which it never predicted.
 */
public record GunKickPayload(boolean off, int cycleMs, float kickDeg) implements CustomPacketPayload {

    public static final Type<GunKickPayload> TYPE = new Type<>(Feedback.id("gun_kick"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GunKickPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, GunKickPayload::off,
            ByteBufCodecs.VAR_INT, GunKickPayload::cycleMs,
            ByteBufCodecs.FLOAT, GunKickPayload::kickDeg,
            GunKickPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Server side: kick {@code player}'s view as {@code figures} would. */
    public static void send(ServerPlayer player, boolean off, GunFigures figures) {
        PacketDistributor.sendToPlayer(player, new GunKickPayload(off, figures.cycleMs(), figures.kickDeg()));
    }

    public static void handle(GunKickPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> GunClient.serverKick(payload.off(), payload.cycleMs(), payload.kickDeg()));
    }
}
