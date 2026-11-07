// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.process.ClientTable;
import io.github.soundgoodizerfan.feedback.village.Good;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Ships the goods table so a recipe browser can print each profession's shop. */
public record GoodsSyncPayload(List<Good> entries) implements CustomPacketPayload {

    public static final Type<GoodsSyncPayload> TYPE = new Type<>(Feedback.id("goods_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GoodsSyncPayload> STREAM_CODEC =
            ByteBufCodecs.fromCodecWithRegistries(Good.CODEC.listOf()).map(GoodsSyncPayload::new, GoodsSyncPayload::entries);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GoodsSyncPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientTable.GOODS.set(payload.entries()));
    }
}
