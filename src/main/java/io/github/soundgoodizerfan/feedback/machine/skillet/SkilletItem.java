// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.skillet;

import io.github.soundgoodizerfan.feedback.item.FormBlock;
import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** A skillet in some material: the material rides on the stack and passes to the placed block, where it sets how fast the pan swings. */
public class SkilletItem extends BlockItem {

    public SkilletItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static ItemStack of(ResourceLocation material) {
        ItemStack stack = new ItemStack(FItems.SKILLET.get());
        stack.set(FDataComponents.MADE_OF.get(), new MadeOf(material, "plate", 3));
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        return made == null ? super.getName(stack) : FormBlock.name(made.material(), "skillet");
    }
}
