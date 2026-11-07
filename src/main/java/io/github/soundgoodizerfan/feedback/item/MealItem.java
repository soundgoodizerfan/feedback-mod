// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import java.util.List;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** A bowl of what came out of a stockpot. Named from what it is (broth, stew, scorched); the figures are baked on the stack. */
public class MealItem extends Item {

    public MealItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Meal meal = stack.get(FDataComponents.MEAL.get());
        return meal == null ? super.getName(stack) : Component.translatable("item.feedback.meal." + meal.dish());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Meal meal = stack.get(FDataComponents.MEAL.get());
        if (meal != null && meal.pathogen() > FTuning.FOOD_HAZARD_TOLERANCE)
            tooltip.add(Component.translatable("tooltip.feedback.food.pathogen").withStyle(ChatFormatting.RED));
    }
}
