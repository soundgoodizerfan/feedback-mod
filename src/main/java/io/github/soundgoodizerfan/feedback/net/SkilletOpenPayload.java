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

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The server tells the client to open the skillet screen: how hot the fire under the pan is, and the piece (one of the food held). */
public record SkilletOpenPayload(float fireTu, ItemStack piece) implements CustomPacketPayload {

    public static final Type<SkilletOpenPayload> TYPE = new Type<>(Feedback.id("skillet_open"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SkilletOpenPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, SkilletOpenPayload::fireTu,
            ItemStack.STREAM_CODEC, SkilletOpenPayload::piece,
            SkilletOpenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SkilletOpenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> io.github.soundgoodizerfan.feedback.client.SkilletScreen.open(payload.fireTu(), payload.piece()));
    }
}
