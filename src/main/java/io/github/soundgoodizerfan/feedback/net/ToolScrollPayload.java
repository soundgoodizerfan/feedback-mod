// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Shift-scroll on a held tool: steps a Data Connector's mode, or a hand tool's swing, forward ({@code 1}) or
 * back ({@code -1}).
 */
public record ToolScrollPayload(int direction) implements CustomPacketPayload {

    public static final Type<ToolScrollPayload> TYPE = new Type<>(Feedback.id("tool_scroll"));

    public static final StreamCodec<ByteBuf, ToolScrollPayload> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(ToolScrollPayload::new, ToolScrollPayload::direction);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToolScrollPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (io.github.soundgoodizerfan.feedback.modular.item.ModularHandTool.strikes(stack)) {
                var swing = io.github.soundgoodizerfan.feedback.modular.item.ModularHandTool.swing(stack)
                        .step(Integer.signum(payload.direction()), player);
                stack.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.HAND_SWING, swing);
                AimPayloads.modes(player, stack.getHoverName(), AimPayloads.keys("tooltip.feedback.hand_swing.", "tap", "blow", "heavy", "mighty"),
                        swing.ordinal(), Component.translatable("message.feedback.hand_swing.figure",
                                io.github.soundgoodizerfan.feedback.core.unit.Units.figure(io.github.soundgoodizerfan.feedback.modular.item.ModularHandTool.of(stack, swing))));
                return;
            }
            if (stack.getItem() instanceof io.github.soundgoodizerfan.feedback.item.DataConnectorItem) {
                var comp = io.github.soundgoodizerfan.feedback.registry.FDataComponents.DATA_CONNECTOR_MODE.get();
                var mode = stack.getOrDefault(comp, io.github.soundgoodizerfan.feedback.item.DataConnectorMode.LINK)
                        .step(Integer.signum(payload.direction()));
                stack.set(comp, mode);
                // A half-made link means something else in the new mode.
                io.github.soundgoodizerfan.feedback.control.data.DataLinkManager.get().clear(player);
                AimPayloads.linkDone(player);
                AimPayloads.modes(player, stack.getHoverName(), AimPayloads.keys("feedback.data_connector.mode.", "link", "unlink", "clear"),
                        mode.ordinal(), Component.empty());
                return;
            }
        });
    }
}
