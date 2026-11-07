// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.weapon.GunOption;
import io.github.soundgoodizerfan.feedback.weapon.GunOptions;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Ships the gun option table to the client -- {@link MaterialSyncPayload}'s reasoning: the bake runs there too. */
public record GunOptionSyncPayload(Map<ResourceLocation, GunOption> options) implements CustomPacketPayload {

    public static final Type<GunOptionSyncPayload> TYPE = new Type<>(Feedback.id("gun_option_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GunOptionSyncPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.map(HashMap::new, ResourceLocation.STREAM_CODEC, GunOption.STREAM_CODEC),
                    GunOptionSyncPayload::options,
                    GunOptionSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GunOptionSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            GunOptions.setClient(payload.options());
            io.github.soundgoodizerfan.feedback.process.ClientTable.GUN_OPTIONS.set(List.copyOf(payload.options().values()));
        });
    }
}
