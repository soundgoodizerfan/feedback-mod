// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.levitation;

import io.github.soundgoodizerfan.feedback.process.MadeOf;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;
import io.github.soundgoodizerfan.feedback.registry.FItems;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** A levitation plate in some material: the material rides on the stack and passes to the placed block. */
public class LevitationPlateItem extends BlockItem {

    public static final String FORM = "levitation_plate";

    public LevitationPlateItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static ItemStack of(ResourceLocation material) {
        ItemStack stack = new ItemStack(FItems.LEVITATION_PLATE.get());
        stack.set(FDataComponents.MADE_OF.get(), new MadeOf(material, FORM, 1));
        return stack;
    }

    @Override
    public Component getName(ItemStack stack) {
        MadeOf made = stack.get(FDataComponents.MADE_OF.get());
        return made == null ? super.getName(stack)
                : io.github.soundgoodizerfan.feedback.item.FormBlock.name(made.material(), FORM);
    }
}
