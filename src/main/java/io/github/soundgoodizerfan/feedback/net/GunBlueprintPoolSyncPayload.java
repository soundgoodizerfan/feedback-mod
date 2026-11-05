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
import io.github.soundgoodizerfan.feedback.process.ClientTable;
import io.github.soundgoodizerfan.feedback.weapon.GunBlueprintPool;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Ships the gun blueprint pool so a recipe browser can list where blueprints turn up. */
public record GunBlueprintPoolSyncPayload(List<GunBlueprintPool.Listing> entries) implements CustomPacketPayload {

    public static final Type<GunBlueprintPoolSyncPayload> TYPE = new Type<>(Feedback.id("gun_blueprint_pool_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GunBlueprintPoolSyncPayload> STREAM_CODEC =
            GunBlueprintPool.Listing.STREAM_CODEC.apply(ByteBufCodecs.list()).map(GunBlueprintPoolSyncPayload::new, GunBlueprintPoolSyncPayload::entries);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GunBlueprintPoolSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientTable.GUN_BLUEPRINTS.set(payload.entries()));
    }
}
