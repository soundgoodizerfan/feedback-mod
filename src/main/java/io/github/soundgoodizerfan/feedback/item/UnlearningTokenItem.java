// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.combat.skill.Skills;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The respec consumable (plan 5.11: "materials, craftable"). Using it un-buys every skill node and
 * returns the points to their pools; it is spent only if the player owned something. A player-body
 * item: it changes what the player has learned and nothing in the world.
 */
public class UnlearningTokenItem extends Item {

    public UnlearningTokenItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer sp))
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        if (!Skills.respec(sp)) {
            sp.displayClientMessage(Component.translatable("item.feedback.unlearning_token.nothing"), true);
            return InteractionResultHolder.fail(stack);
        }
        stack.consume(1, sp);
        sp.displayClientMessage(Component.translatable("item.feedback.unlearning_token.used"), true);
        return InteractionResultHolder.sidedSuccess(stack, false);
    }
}
