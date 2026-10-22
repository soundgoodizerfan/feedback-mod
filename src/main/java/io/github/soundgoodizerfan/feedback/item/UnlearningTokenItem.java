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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
