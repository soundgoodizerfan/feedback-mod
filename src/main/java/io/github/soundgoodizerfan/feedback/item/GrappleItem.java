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

import io.github.soundgoodizerfan.feedback.combat.tether.GrappleHook;
import io.github.soundgoodizerfan.feedback.modular.ModularTuning;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A hook on a line. Use throws the hook; using it again (or sneaking) lets the line go. What the
 * hook does is {@link GrappleHook}. A cast wears the grapple one point; a line that parts wears it
 * {@link ModularTuning#GRAPPLE_BREAK_WEAR}.
 */
public class GrappleItem extends Item {

    public GrappleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide)
            return InteractionResultHolder.sidedSuccess(stack, true);
        GrappleHook out = GrappleHook.of(level, player);
        if (out != null) {
            out.release();
            return InteractionResultHolder.sidedSuccess(stack, false);
        }
        GrappleHook hook = new GrappleHook(level, player);
        hook.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, ModularTuning.GRAPPLE_SPEED, 0f);
        level.addFreshEntity(hook);
        level.playSound(null, player, SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 0.7f, 0.9f);
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        return InteractionResultHolder.sidedSuccess(stack, false);
    }

    /** Wears whichever grapple {@code player} holds. */
    public static void wear(Player player, int amount) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack s = player.getItemInHand(hand);
            if (s.getItem() instanceof GrappleItem) {
                s.hurtAndBreak(amount, player, LivingEntity.getSlotForHand(hand));
                return;
            }
        }
    }
}
