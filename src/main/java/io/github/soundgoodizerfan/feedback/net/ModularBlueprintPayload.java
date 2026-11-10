// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.modular.BlueprintTableMenu;
import io.github.soundgoodizerfan.feedback.modular.ModularDesign;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The Blueprint Table screen's Print (also the Prep Table's Blueprint tab's): the design drawn, and the drafts pinned beside it. */
public record ModularBlueprintPayload(ModularDesign design, java.util.List<ModularDesign> variants) implements CustomPacketPayload {

    public static final Type<ModularBlueprintPayload> TYPE = new Type<>(Feedback.id("modular_blueprint"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ModularBlueprintPayload> STREAM_CODEC =
            StreamCodec.composite(ModularDesign.STREAM_CODEC, ModularBlueprintPayload::design,
                    ModularDesign.STREAM_CODEC.apply(net.minecraft.network.codec.ByteBufCodecs.list(BlueprintTableMenu.MAX_VARIANTS)), ModularBlueprintPayload::variants,
                    ModularBlueprintPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ModularBlueprintPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player().containerMenu instanceof BlueprintTableMenu menu)
                menu.print(payload.design(), payload.variants(), context.player());
            else if (context.player().containerMenu instanceof io.github.soundgoodizerfan.feedback.modular.ToolTableMenu bench)
                bench.printBlueprint(payload.design(), payload.variants(), context.player());
        });
    }
}
