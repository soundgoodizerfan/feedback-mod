// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.item;

import java.util.List;
import java.util.Locale;

import io.github.soundgoodizerfan.feedback.item.RadioSetItem;
import io.github.soundgoodizerfan.feedback.radio.RadioSets;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A radio antenna: a whip cut to a length, with an optional loading coil of wire. Its figures are baked by {@code BlockFigures} (case
 * {@code antenna}); used with a radio set in the other hand it is fitted to the set ({@link RadioSets#attach}) and used up, the set keeping the
 * effective length and the conductor's conductivity.
 */
public class ModularAntenna extends ModularItem {

    public ModularAntenna(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack antenna = player.getItemInHand(hand);
        ItemStack set = player.getItemInHand(hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!(set.getItem() instanceof RadioSetItem) || ModularStacks.broken(antenna))
            return InteractionResultHolder.pass(antenna);
        if (!level.isClientSide && RadioSets.attach(set, antenna)) {
            antenna.shrink(1);
            player.displayClientMessage(Component.translatable("message.feedback.radio.antenna_fitted", RadioSets.antennaMm(set)), true);
        }
        return InteractionResultHolder.sidedSuccess(antenna, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CompoundTag figs = stack.get(FDataComponents.BLOCK_FIGURES.get());
        if (figs != null)
            for (String key : figs.getAllKeys())
                tooltip.add(Component.translatable("figure.feedback.block." + key, String.format(Locale.ROOT, "%.3g", figs.getFloat(key))));
    }
}
