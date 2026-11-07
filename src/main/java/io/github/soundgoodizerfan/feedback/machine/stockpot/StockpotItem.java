// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.stockpot;

import io.github.soundgoodizerfan.feedback.item.FormBlock;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** A stockpot in some material: the material rides on the stack and passes to the placed block, where it sets how fast the pot follows its fire. */
public class StockpotItem extends BlockItem {

    public StockpotItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static ItemStack of(ResourceLocation material) {
        ItemStack stack = new ItemStack(FItems.STOCKPOT.get());
        stack.set(FDataComponents.MADE_OF.get(), new MadeOf(material, "plate", 5));
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        return made == null ? super.getName(stack) : FormBlock.name(made.material(), "stockpot");
    }
}
