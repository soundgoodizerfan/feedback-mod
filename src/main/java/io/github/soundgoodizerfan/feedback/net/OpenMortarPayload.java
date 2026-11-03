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
import io.github.soundgoodizerfan.feedback.item.MortarMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The Mortar tab on the inventory screen was clicked: open the Mortar's bench. */
public record OpenMortarPayload() implements CustomPacketPayload {

    public static final Type<OpenMortarPayload> TYPE = new Type<>(Feedback.id("open_mortar"));

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenMortarPayload> STREAM_CODEC = StreamCodec.unit(new OpenMortarPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(OpenMortarPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> context.player().openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new MortarMenu(id, inventory), Component.translatable("item.feedback.mortar"))));
    }
}
