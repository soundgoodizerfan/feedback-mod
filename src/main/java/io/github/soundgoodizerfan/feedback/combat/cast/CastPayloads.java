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
package io.github.soundgoodizerfan.feedback.combat.cast;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** What the client says about casting: fire a slot, or slot a technique. The server checks everything. */
public final class CastPayloads {

    private CastPayloads() {
    }

    public record Fire(int slot) implements CustomPacketPayload {
        public static final Type<Fire> TYPE = new Type<>(Feedback.id("cast_fire"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Fire> STREAM_CODEC =
                ByteBufCodecs.VAR_INT.map(Fire::new, Fire::slot).cast();

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Fire p, IPayloadContext context) {
            context.enqueueWork(() -> Casts.fire((ServerPlayer) context.player(), p.slot()));
        }
    }

    public record Slot(int slot, String tech) implements CustomPacketPayload {
        public static final Type<Slot> TYPE = new Type<>(Feedback.id("cast_slot"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Slot> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Slot::slot, ByteBufCodecs.STRING_UTF8, Slot::tech, Slot::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(Slot p, IPayloadContext context) {
            context.enqueueWork(() -> Casts.slot((ServerPlayer) context.player(), p.slot(), p.tech()));
        }
    }

    /** Server to client: ease the view toward this yaw and pitch (a cast that turns the player, like Mark and Fire's sweep). */
    public record Turn(float yaw, float pitch) implements CustomPacketPayload {
        public static final Type<Turn> TYPE = new Type<>(Feedback.id("cast_turn"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Turn> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.FLOAT, Turn::yaw, ByteBufCodecs.FLOAT, Turn::pitch, Turn::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        /** Only ever runs on the client; the work is in {@code client/}. */
        public static void handle(Turn p, IPayloadContext context) {
            context.enqueueWork(() -> io.github.soundgoodizerfan.feedback.client.ClientPayloads.turn(p.yaw(), p.pitch()));
        }
    }
}
