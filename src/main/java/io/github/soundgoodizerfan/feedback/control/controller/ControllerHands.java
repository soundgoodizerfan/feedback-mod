// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.controller;

import java.util.Optional;

import io.github.soundgoodizerfan.feedback.fitting.Fittable;
import io.github.soundgoodizerfan.feedback.registry.FTags;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** What a hand does to a block that hosts a controller: a medium in, the medium back out, the contents on breaking. Shared by every host block. */
public final class ControllerHands {

    private ControllerHands() {
    }

    /** A program medium in hand goes into the slot, if this host takes that medium and the card fits its limits. */
    public static ItemInteractionResult insert(ItemStack stack, Level level, BlockPos pos, Player player) {
        if (!stack.is(FTags.PUNCH_CARDS) || player.isShiftKeyDown())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof ControllerHost controller) || !controller.getItem(0).isEmpty())
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!controller.core().takesMedium(stack)) {
            if (!level.isClientSide)
                player.displayClientMessage(Component.translatable("feedback.controller.wrong_medium"), true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        Optional<String> refusal = controller.refusalFor(stack);
        if (refusal.isPresent()) {
            if (!level.isClientSide)
                player.displayClientMessage(Component.translatable("feedback.controller.card_refused",
                        Component.translatable("feedback.programmer.error." + refusal.get())), true);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide)
            controller.setItem(0, stack.split(1));
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    /** An empty hand takes the medium back out. */
    public static InteractionResult take(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof ControllerHost controller))
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            ItemStack taken = controller.removeItemNoUpdate(0);
            if (!taken.isEmpty() && !player.getInventory().add(taken))
                player.drop(taken, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static void dropContents(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof ControllerHost controller) {
            Containers.dropContents(level, pos, controller);
            Fittable.dropAll(controller, level, pos);
        }
    }
}
