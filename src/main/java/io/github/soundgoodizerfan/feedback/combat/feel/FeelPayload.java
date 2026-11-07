// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
