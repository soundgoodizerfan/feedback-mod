// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import io.github.soundgoodizerfan.feedback.core.FTuning;
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

/** The battery as an item: a modular build that places as {@code BatteryBlock} and carries its state ({@link FDataComponents#BATTERY}). */
public class BatteryItem extends BlockItem implements Modular {

    public BatteryItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return ModularStacks.name(stack, super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ModularTooltip.add(stack, tooltip);
        CompoundTag state = stack.get(FDataComponents.BATTERY.get());
        if (state != null) {
            tooltip.add(Component.translatable("tooltip.feedback.battery.cells", state.getInt("Cells")));
            tooltip.add(Component.translatable(state.getFloat("Acid") >= state.getInt("Cells") * io.github.soundgoodizerfan.feedback.core.FTuning.BATTERY_ACID_MB_PER_CELL - 0.5f
                    ? "tooltip.feedback.battery.filled" : "tooltip.feedback.battery.dry"));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        ModularStacks.tick(stack, level);
    }
}
