// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.hud.SizeUpHud;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Every creature the player has read, as the server sees it: which, how long the read has left, what it is giving away
 * (a bit set, see {@code Weakness.OPENING_WORDS}), and whether an exploit or a soft spot is armed on it. Sent when a read
 * is made and every few ticks after; an empty list clears the HUD. Health and armour the client reads off the entity.
 */
public record SizeUpPayload(List<Entry> reads) implements CustomPacketPayload {

    /** @param flags bit 0: exploit armed; bits 1-3: soft spot slot (0 none, 1 head, 2 chest, 3 legs, 4 feet); bit 4: known type. */
    public record Entry(int id, int left, int mask, int flags, int hits) {
    }

    public static final Type<SizeUpPayload> TYPE = new Type<>(Feedback.id("size_up"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SizeUpPayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeVarInt(p.reads.size());
                for (Entry e : p.reads) {
                    buf.writeVarInt(e.id);
                    buf.writeVarInt(e.left);
                    buf.writeVarInt(e.mask);
                    buf.writeVarInt(e.flags);
                    buf.writeVarInt(e.hits);
                }
            },
            buf -> {
                int n = buf.readVarInt();
                List<Entry> out = new ArrayList<>();
                for (int i = 0; i < n; i++)
                    out.add(new Entry(buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
                return new SizeUpPayload(out);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void send(ServerPlayer player, List<Entry> reads) {
        PacketDistributor.sendToPlayer(player, new SizeUpPayload(reads));
    }

    public static void handle(SizeUpPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> SizeUpHud.take(payload.reads()));
    }
}
