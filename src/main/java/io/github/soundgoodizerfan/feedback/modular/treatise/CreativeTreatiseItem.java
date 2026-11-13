// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.treatise;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Every volume of every school's treatise at once, including volumes added later ({@link Treatises#holds} answers true for it, and the
 * masks loop over {@link School#volumes()}). A test and operator item: holding it lets the table draw anything.
 */
public class CreativeTreatiseItem extends Item {

    public CreativeTreatiseItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.feedback.creative_treatise").withStyle(ChatFormatting.GRAY));
    }
}
