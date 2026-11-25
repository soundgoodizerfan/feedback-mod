// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import java.util.List;

import io.github.soundgoodizerfan.feedback.registry.FStore;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A bay fitting in the hand; used on an empty rack bay to install it. */
public class FittingItem extends Item {

    private final Fitting fitting;

    public FittingItem(Fitting fitting, Properties properties) {
        super(properties);
        this.fitting = fitting;
    }

    public Fitting fitting() {
        return fitting;
    }

    public static Item of(Fitting fitting) {
        return FStore.FITTINGS.get(fitting).get();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.feedback.rack_fitting." + fitting.id()).withStyle(ChatFormatting.GRAY));
    }
}
