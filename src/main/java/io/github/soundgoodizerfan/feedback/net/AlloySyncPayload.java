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
import io.github.soundgoodizerfan.feedback.process.AlloyRecipe;
import io.github.soundgoodizerfan.feedback.process.ClientTable;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Ships the alloy table so a recipe browser can print it -- {@link CastingSyncPayload}'s reasoning. */
public record AlloySyncPayload(List<AlloyRecipe> entries) implements CustomPacketPayload {

    public static final Type<AlloySyncPayload> TYPE = new Type<>(Feedback.id("alloy_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, AlloySyncPayload> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(AlloyRecipe.CODEC.listOf())
                    .map(AlloySyncPayload::new, AlloySyncPayload::entries);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AlloySyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientTable.ALLOYS.set(payload.entries()));
    }
}
