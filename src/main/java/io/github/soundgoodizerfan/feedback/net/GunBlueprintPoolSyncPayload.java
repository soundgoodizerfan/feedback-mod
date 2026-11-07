// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
