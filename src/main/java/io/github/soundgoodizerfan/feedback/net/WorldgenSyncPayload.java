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
import io.github.soundgoodizerfan.feedback.geology.Basin;
import io.github.soundgoodizerfan.feedback.process.ClientTable;
import io.github.soundgoodizerfan.feedback.worldgen.OreVein;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Ships the ore-vein and basin tables so a recipe browser can list where things occur -- {@link DipSyncPayload}'s reasoning. */
public record WorldgenSyncPayload(List<OreVein> veins, List<Basin> basins) implements CustomPacketPayload {

    public static final Type<WorldgenSyncPayload> TYPE = new Type<>(Feedback.id("worldgen_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, WorldgenSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.fromCodecWithRegistries(OreVein.CODEC.listOf()), WorldgenSyncPayload::veins,
            ByteBufCodecs.fromCodecWithRegistries(Basin.CODEC.listOf()), WorldgenSyncPayload::basins,
            WorldgenSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WorldgenSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            ClientTable.ORE_VEINS.set(payload.veins());
            ClientTable.BASINS.set(payload.basins());
        });
    }
}
