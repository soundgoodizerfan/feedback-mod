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
import io.github.soundgoodizerfan.feedback.client.HudReadouts;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * A short line for the HUD readout stack ({@link HudReadouts}) in place of the vanilla action bar:
 * the text, how long it stays in ticks, and a priority that decides which of a full stack is
 * dropped first. Sent through {@link HudReadouts#send}.
 */
public record HudReadoutPayload(Component text, int ticks, int priority) implements CustomPacketPayload {

    public static final Type<HudReadoutPayload> TYPE = new Type<>(Feedback.id("hud_readout"));

    public static final StreamCodec<RegistryFriendlyByteBuf, HudReadoutPayload> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC, HudReadoutPayload::text,
            ByteBufCodecs.VAR_INT, HudReadoutPayload::ticks,
            ByteBufCodecs.VAR_INT, HudReadoutPayload::priority,
            HudReadoutPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HudReadoutPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> HudReadouts.add(payload.text(), payload.ticks(), payload.priority()));
    }
}
