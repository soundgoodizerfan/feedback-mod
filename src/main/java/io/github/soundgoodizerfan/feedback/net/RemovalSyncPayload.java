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

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.ClientRemovals;
import io.github.soundgoodizerfan.feedback.process.Removal;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Ships the removal table to the client -- {@link DeformationSyncPayload}'s own reasoning. */
public record RemovalSyncPayload(List<Removal> entries) implements CustomPacketPayload {

    public static final Type<RemovalSyncPayload> TYPE = new Type<>(Feedback.id("removal_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RemovalSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    Removal.STREAM_CODEC.apply(ByteBufCodecs.list()), RemovalSyncPayload::entries,
                    RemovalSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RemovalSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientRemovals.set(payload.entries()));
    }
}
