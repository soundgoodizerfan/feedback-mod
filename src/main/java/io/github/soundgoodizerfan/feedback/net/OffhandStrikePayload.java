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
import io.github.soundgoodizerfan.feedback.combat.hit.OffhandStrike;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The off-hand attack key was pressed, or in barehanded mode either fist struck ({@link OffhandStrike}). */
public record OffhandStrikePayload(boolean main) implements CustomPacketPayload {

    public static final Type<OffhandStrikePayload> TYPE = new Type<>(Feedback.id("offhand_strike"));

    public static final StreamCodec<ByteBuf, OffhandStrikePayload> STREAM_CODEC = ByteBufCodecs.BOOL.map(OffhandStrikePayload::new, OffhandStrikePayload::main);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OffhandStrikePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> OffhandStrike.strike((ServerPlayer) context.player(), payload.main()));
    }
}
