// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.List;

import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** The conveyor belt as an item: a modular build that places as {@code BeltBlock} and carries its band ({@link FDataComponents#BELT}). */
public class BeltItem extends BlockItem implements Modular {

    public BeltItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return ModularStacks.name(stack, super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ModularTooltip.add(stack, tooltip);
        CompoundTag state = stack.get(FDataComponents.BELT.get());
        if (state != null && state.getBoolean("Cleat"))
            tooltip.add(Component.translatable("tooltip.feedback.belt.cleated"));
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        ModularStacks.tick(stack, level);
    }
}
