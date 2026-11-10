// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.List;
import java.util.Locale;

import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** The pressure vessel as an item: a modular build that places as {@code PressureVesselBlock} and carries its baked figures ({@link FDataComponents#PRESSURE_VESSEL}). */
public class PressureVesselItem extends BlockItem implements Modular {

    public PressureVesselItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return ModularStacks.name(stack, super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ModularTooltip.add(stack, tooltip);
        CompoundTag state = stack.get(FDataComponents.PRESSURE_VESSEL.get());
        if (state != null) {
            tooltip.add(Component.translatable("tooltip.feedback.pressure_vessel.room", String.format(Locale.ROOT, "%.0f", state.getFloat("Volume"))));
            tooltip.add(Component.translatable("tooltip.feedback.pressure_vessel.danger", String.format(Locale.ROOT, "%.1f", state.getFloat("Danger"))));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        ModularStacks.tick(stack, level);
    }
}
