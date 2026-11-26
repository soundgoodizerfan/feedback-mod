// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.registry.FWeapons;
import io.github.soundgoodizerfan.feedback.weapon.GunTuning;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A steel rod with a rag at its tip that pushes a lodged round out of a bore. Held in one hand with the gun in the other, use it: the lodged round
 * comes out and the hand is busy for {@link GunTuning#GUN_CLEAR_TICKS} ticks (a lost turn, not a lost gun). With nothing lodged it does nothing.
 */
public class CleaningRodItem extends Item {

    public CleaningRodItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack rod = player.getItemInHand(hand);
        ItemStack gun = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!gun.getOrDefault(FWeapons.GUN_LODGED.get(), false))
            return InteractionResultHolder.pass(rod);
        if (!level.isClientSide) {
            gun.remove(FWeapons.GUN_LODGED.get());
            player.displayClientMessage(Component.translatable("gun.feedback.cleared"), true);
            player.getCooldowns().addCooldown(this, GunTuning.GUN_CLEAR_TICKS);
        }
        return InteractionResultHolder.sidedSuccess(rod, level.isClientSide);
    }
}
