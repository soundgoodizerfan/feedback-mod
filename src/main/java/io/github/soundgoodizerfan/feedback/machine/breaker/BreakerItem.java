// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.breaker;

import java.util.List;

import io.github.soundgoodizerfan.feedback.machine.cable.CableGauge;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

/** The breaker's item: carries a cable gauge ({@code CABLE_GAUGE}) and says which. */
public class BreakerItem extends BlockItem {

    public BreakerItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        CableGauge gauge = stack.get(FDataComponents.CABLE_GAUGE.get());
        if (gauge != null)
            tooltip.add(Component.translatable("feedback.tooltip.cable.gauge", Component.translatable(gauge.key()),
                    Component.literal(String.valueOf(gauge.area()))).withStyle(ChatFormatting.GRAY));
    }
}
