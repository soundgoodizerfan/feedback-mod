// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.air;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** A Propeller in hand: a modular build (the {@code propeller} type) carrying the design it bakes. */
public class PropItem extends BlockItem implements io.github.soundgoodizerfan.feedback.modular.Modular {

    public PropItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return io.github.soundgoodizerfan.feedback.modular.ModularStacks.name(stack, super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        io.github.soundgoodizerfan.feedback.modular.ModularTooltip.add(stack, tooltip);
    }
}
