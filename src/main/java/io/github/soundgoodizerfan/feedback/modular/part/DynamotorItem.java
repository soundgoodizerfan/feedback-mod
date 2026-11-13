// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.part;

import java.util.List;

import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTooltip;
import io.github.soundgoodizerfan.feedback.modular.item.ModularStacks;
import io.github.soundgoodizerfan.feedback.modular.type.Modular;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** The motor and dynamo as an item: a modular build that places as {@code DynamotorBlock} and carries its state ({@link FDataComponents#DYNAMOTOR}). */
public class DynamotorItem extends BlockItem implements Modular {

    public DynamotorItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        return ModularStacks.name(stack, super.getName(stack));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ModularTooltip.add(stack, tooltip);
        CompoundTag state = stack.get(FDataComponents.DYNAMOTOR.get());
        if (state != null) {
            tooltip.add(Component.translatable("tooltip.feedback.dynamotor.band",
                    Component.translatable("feedback.unit.eu.band." + state.getString("Band"))));
            tooltip.add(Component.translatable("tooltip.feedback.dynamotor.amps", Math.round(state.getFloat("RatedAmps"))));
            if (state.getBoolean("Jacketed"))
                tooltip.add(Component.translatable("tooltip.feedback.dynamotor.jacket"));
        }
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        ModularStacks.tick(stack, level);
    }
}
