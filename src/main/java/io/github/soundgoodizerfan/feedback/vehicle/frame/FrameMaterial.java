// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.frame;

import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

/**
 * What a stack held in the off hand offers a frame as plate or tire stock. A frame records its
 * material as a block id (its sprite, density and friction all hang off that), so a block is its
 * own answer and a <em>plate</em> form item answers with its material's block form. Any other
 * form answers nothing: a plate is the form a sheet of chassis or a tread is made of.
 */
final class FrameMaterial {

    private FrameMaterial() {}

    /** The block id {@code stack} stands for as plate or tire stock, or null if it is neither a block nor a plate. */
    @Nullable
    static ResourceLocation plate(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem block)
            return BuiltInRegistries.BLOCK.getKey(block.getBlock());
        return Material.find(MaterialTable.get().materials(), stack)
                .filter(found -> found.form().equals("plate"))
                .flatMap(found -> found.material().item(found.id(), "block"))
                .filter(BlockItem.class::isInstance)
                .map(item -> BuiltInRegistries.BLOCK.getKey(((BlockItem) item).getBlock()))
                .orElse(null);
    }
}
