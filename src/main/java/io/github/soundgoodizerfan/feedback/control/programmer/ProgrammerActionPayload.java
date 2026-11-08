// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.programmer;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.control.program.ControllerLimits;
import io.github.soundgoodizerfan.feedback.control.program.ProgramGraph;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Client -> server: two things the Programmer screen asks that are not an edit. {@link #ERASE} wipes a printed card of a soft medium
 * (a Floppy Disk or USB Drive) back into the working copy so it can be authored again and printed over. {@link #SAVE_FUNCTION}
 * writes a selection of cards onto a blank Function Card in the player's hand; stamping one back is client-side, since the card's
 * contents are already on the item.
 */
public record ProgrammerActionPayload(BlockPos pos, int action, ProgramGraph graph) implements CustomPacketPayload {

    public static final int ERASE = 0, SAVE_FUNCTION = 1;

    public static final Type<ProgrammerActionPayload> TYPE = new Type<>(Feedback.id("programmer_action"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProgrammerActionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, ProgrammerActionPayload::pos,
            ByteBufCodecs.VAR_INT, ProgrammerActionPayload::action,
            ProgramGraph.STREAM_CODEC.cast(), ProgrammerActionPayload::graph,
            ProgrammerActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ProgrammerActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().containerMenu instanceof ProgrammerMenu menu) || !menu.getPos().equals(payload.pos()))
                return;
            var player = context.player();
            if (payload.action() == ERASE) {
                if (player.level().getBlockEntity(payload.pos()) instanceof ProgrammerBlockEntity programmer)
                    programmer.erase(player);
            } else if (payload.action() == SAVE_FUNCTION) {
                if (payload.graph().nodes().isEmpty() || payload.graph().findEditError(ControllerLimits.MAX).isPresent()) {
                    player.displayClientMessage(Component.translatable("feedback.programmer.function_bad"), true);
                    return;
                }
                for (InteractionHand hand : InteractionHand.values()) {
                    ItemStack held = player.getItemInHand(hand);
                    if (held.is(FItems.FUNCTION_CARD.get())
                            && held.getOrDefault(FDataComponents.PROGRAM_GRAPH, ProgramGraph.EMPTY).nodes().isEmpty()) {
                        held.set(FDataComponents.PROGRAM_GRAPH, payload.graph());
                        player.displayClientMessage(Component.translatable("feedback.programmer.function_saved"), true);
                        return;
                    }
                }
                player.displayClientMessage(Component.translatable("feedback.programmer.function_no_card"), true);
            }
        });
    }
}
