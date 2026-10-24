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
import io.github.soundgoodizerfan.feedback.client.UpswingSwing;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** {@link SwingStartPayload}, relayed to everyone who can see the swinger (never the swinger, who played it already). */
public record SwingPlayPayload(int entity, String clip, int hand, float length, float upswing, String sound)
        implements CustomPacketPayload {

    public static final Type<SwingPlayPayload> TYPE = new Type<>(Feedback.id("swing_play"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SwingPlayPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SwingPlayPayload::entity,
            ByteBufCodecs.STRING_UTF8, SwingPlayPayload::clip,
            ByteBufCodecs.VAR_INT, SwingPlayPayload::hand,
            ByteBufCodecs.FLOAT, SwingPlayPayload::length,
            ByteBufCodecs.FLOAT, SwingPlayPayload::upswing,
            ByteBufCodecs.STRING_UTF8, SwingPlayPayload::sound,
            SwingPlayPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SwingPlayPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> UpswingSwing.playRemote(payload));
    }
}
