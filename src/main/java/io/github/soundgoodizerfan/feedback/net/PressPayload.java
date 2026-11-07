// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.weapon.ReloadingPressBlockEntity;
import io.github.soundgoodizerfan.feedback.weapon.ReloadingPressMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The Reloading Press screen: the die set's bore and whether the lever was pulled. */
public record PressPayload(BlockPos pos, int bore, boolean seat) implements CustomPacketPayload {

    public static final Type<PressPayload> TYPE = new Type<>(Feedback.id("reloading_press"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PressPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, PressPayload::pos,
            ByteBufCodecs.VAR_INT, PressPayload::bore,
            ByteBufCodecs.BOOL, PressPayload::seat,
            PressPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PressPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof ReloadingPressMenu menu && menu.getPos().equals(payload.pos())
                    && context.player().level().getBlockEntity(payload.pos()) instanceof ReloadingPressBlockEntity press)
                press.commit(payload.bore(), payload.seat(), context.player());
        });
    }
}
