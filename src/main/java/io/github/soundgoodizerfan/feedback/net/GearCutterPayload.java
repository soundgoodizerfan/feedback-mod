// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.gearcutter.GearCutterBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The tooth count picked on the Gear Cutter's screen. The server checks the player is at the machine and that the
 * count is one of the options before it sets it.
 */
public record GearCutterPayload(BlockPos pos, int teeth) implements CustomPacketPayload {

    public static final Type<GearCutterPayload> TYPE = new Type<>(Feedback.id("gear_cutter"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GearCutterPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, GearCutterPayload::pos,
            ByteBufCodecs.VAR_INT, GearCutterPayload::teeth,
            GearCutterPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GearCutterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player))
                return;
            if (player.distanceToSqr(payload.pos().getCenter()) > 64.0
                    || !(player.level().getBlockEntity(payload.pos()) instanceof GearCutterBlockEntity cutter))
                return;
            cutter.setTeeth(payload.teeth());
        });
    }
}
