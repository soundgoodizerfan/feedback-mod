/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.net;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import io.github.soundgoodizerfan.feedback.vehicle.frame.WelderMode;
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
 * Shift-scroll on a held tool: steps a Welder's mode, or a hand tool's swing, forward ({@code 1}) or
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
            if (stack.getItem() instanceof io.github.soundgoodizerfan.feedback.item.HandHammerItem) {
                var swing = io.github.soundgoodizerfan.feedback.item.HandHammerItem.swing(stack)
                        .step(Integer.signum(payload.direction()), player);
                stack.set(io.github.soundgoodizerfan.feedback.registry.FDataComponents.HAND_SWING, swing);
                player.displayClientMessage(Component.translatable("message.feedback.hand_swing",
                        Component.translatable("tooltip.feedback.hand_swing." + swing.name().toLowerCase(java.util.Locale.ROOT)),
                        io.github.soundgoodizerfan.feedback.core.unit.Units.figure(swing.strength())), true);
                return;
            }
            if (io.github.soundgoodizerfan.feedback.modular.ModularStacks.edged(stack)) {
                var edge = io.github.soundgoodizerfan.feedback.modular.ModularStacks.grind(stack, Integer.signum(payload.direction()));
                if (edge != null)
                    player.displayClientMessage(Component.translatable("message.feedback.modular.edge",
                            Component.translatable("tooltip.feedback.modular.edge." + edge.getSerializedName())), true);
                return;
            }
            if (!stack.is(FVehicles.WELDER.get()))
                return;
            WelderMode now = stack.getOrDefault(FVehicles.WELDER_MODE.get(), WelderMode.POINT)
                    .step(Integer.signum(payload.direction()));
            stack.set(FVehicles.WELDER_MODE.get(), now);
            // A half-made selection means something else in the new mode.
            stack.remove(FVehicles.WELDER_SELECTION.get());
            stack.remove(FVehicles.WELDER_PICKS.get());
            player.displayClientMessage(Component.translatable("message.feedback.welder.mode",
                    Component.translatable("message.feedback.welder.mode." + now.name().toLowerCase(java.util.Locale.ROOT))), true);
        });
    }
}
