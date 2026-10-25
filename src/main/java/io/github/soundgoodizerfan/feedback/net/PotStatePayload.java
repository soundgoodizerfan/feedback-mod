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

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The server tells the client how the pot is: scum on top, how mixed it is, how scorched, and whether to open the stirring screen. */
public record PotStatePayload(BlockPos pos, boolean open, float scum, float mixed, float scorch, float water) implements CustomPacketPayload {

    public static final Type<PotStatePayload> TYPE = new Type<>(Feedback.id("pot_state"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PotStatePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PotStatePayload::pos,
            ByteBufCodecs.BOOL, PotStatePayload::open,
            ByteBufCodecs.FLOAT, PotStatePayload::scum,
            ByteBufCodecs.FLOAT, PotStatePayload::mixed,
            ByteBufCodecs.FLOAT, PotStatePayload::scorch,
            ByteBufCodecs.FLOAT, PotStatePayload::water,
            PotStatePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PotStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> io.github.soundgoodizerfan.feedback.client.PotScreen.state(payload));
    }
}
