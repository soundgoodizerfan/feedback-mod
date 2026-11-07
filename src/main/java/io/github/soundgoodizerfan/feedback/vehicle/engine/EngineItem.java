// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.engine;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** An Engine block in hand: a modular build (the {@code engine} type) carrying the design it bakes (spec section 6). */
public class EngineItem extends BlockItem implements io.github.soundgoodizerfan.feedback.modular.Modular {

    public EngineItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return io.github.soundgoodizerfan.feedback.modular.ModularStacks.name(stack, super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        io.github.soundgoodizerfan.feedback.modular.ModularTooltip.add(stack, tooltip);
        EngineTooltip.add(stack, context, tooltip);
    }
}
