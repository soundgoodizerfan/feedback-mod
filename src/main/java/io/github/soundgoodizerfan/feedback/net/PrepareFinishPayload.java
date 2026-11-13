// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;
import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.bench.PartBenchMenu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * What the player did at a Prep Table minigame: the cut's mark positions (0..1000), the knead's press samples (0..100) or the pour's held ticks. The menu
 * recomputes the class from these and refuses a result that came faster than it could be played
 * ({@link PartBenchMenu#finishPrepare}); no grade ever comes from the client.
 */
public record PrepareFinishPayload(int container, int kind, List<Integer> marks, int heldTicks) implements CustomPacketPayload {

    public static final Type<PrepareFinishPayload> TYPE = new Type<>(Feedback.id("prepare_finish"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PrepareFinishPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, PrepareFinishPayload::container,
            ByteBufCodecs.VAR_INT, PrepareFinishPayload::kind,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(Math.max(ModularTuning.CUT_MAX_MARKS, ModularTuning.KNEAD_MAX_SAMPLES))), PrepareFinishPayload::marks,
            ByteBufCodecs.VAR_INT, PrepareFinishPayload::heldTicks,
            PrepareFinishPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PrepareFinishPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player && player.containerMenu instanceof PartBenchMenu menu
                    && menu.containerId == payload.container())
                menu.finishPrepare(player, payload.kind(), payload.marks(), payload.heldTicks());
        });
    }
}
