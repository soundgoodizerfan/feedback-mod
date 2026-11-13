// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.part;

import io.github.soundgoodizerfan.feedback.core.xp.XpStore;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.modular.item.ModularItem;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** The vial: using it pours every point it holds into the player, whose levels follow vanilla's curve. */
public class XpContainerItem extends ModularItem {

    public XpContainerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        XpStore held = stack.get(FDataComponents.XP_STORE.get());
        if (held == null || held.points() <= 0 || player.isShiftKeyDown())
            return super.use(level, player, hand);
        if (!level.isClientSide) {
            player.giveExperiencePoints(held.points());
            stack.set(FDataComponents.XP_STORE.get(), held.with(0));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
