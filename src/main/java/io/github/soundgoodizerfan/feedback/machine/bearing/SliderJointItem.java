// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.bearing;

import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import java.util.List;

/** A Slider Joint; with a spring metal fitted ({@link SliderSpringRecipe}) it says which. */
public class SliderJointItem extends BlockItem {

    public SliderJointItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        var metal = stack.get(FDataComponents.SLIDER_SPRING.get());
        if (metal != null)
            tooltip.add(Component.translatable("tooltip.feedback.slider_spring", Component.translatable("material." + metal.getNamespace() + "." + metal.getPath()),
                    Component.translatable("message.feedback.slider.band." + SliderSpringRecipe.band(metal))));
    }
}
