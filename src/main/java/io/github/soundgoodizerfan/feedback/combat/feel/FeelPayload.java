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
package io.github.soundgoodizerfan.feedback.combat.feel;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.FeelClient;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * An impact, server to client. {@code local} is true for the one party it happened to (camera
 * and all) and false for a bystander (flash, sparks and sound only).
 */
public record FeelPayload(Feel feel, Vec3 at, boolean local) implements CustomPacketPayload {

    public static final Type<FeelPayload> TYPE = new Type<>(Feedback.id("feel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FeelPayload> STREAM_CODEC = StreamCodec.composite(
            Feel.STREAM_CODEC, FeelPayload::feel,
            StreamCodec.<RegistryFriendlyByteBuf, Vec3>of(
                    (buf, v) -> buf.writeDouble(v.x).writeDouble(v.y).writeDouble(v.z),
                    buf -> new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())), FeelPayload::at,
            ByteBufCodecs.BOOL, FeelPayload::local,
            FeelPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FeelPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> FeelClient.play(payload.feel().clamped(), payload.at(), payload.local()));
    }
}
