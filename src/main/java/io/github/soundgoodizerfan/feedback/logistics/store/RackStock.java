// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * What a broken rack keeps on its item ({@link FDataComponents#RACK_STOCK}): the bays as the block saved them, and their
 * mass worked out then, so the item can be weighed without reading the lots back.
 */
public final class RackStock {

    public static final String BAYS = "Bays", MASS = "Mass";

    private RackStock() {
    }

    /** Drops everything on a destroyed rack item's shelves where the item was. */
    public static void spill(ItemStack stack, Level level, double x, double y, double z) {
        CompoundTag tag = stack.get(FDataComponents.RACK_STOCK.get());
        if (tag == null)
            return;
        ListTag bays = tag.getList(BAYS, Tag.TAG_COMPOUND);
        for (int i = 0; i < bays.size(); i++) {
            Bay bay = new Bay();
            bay.load(bays.getCompound(i), level.registryAccess());
            for (Lot l : bay.lots)
                while (l.count > 0) {
                    int n = Math.min(l.count, l.item.getMaxStackSize());
                    Containers.dropItemStack(level, x, y, z, l.item.copyWithCount(n));
                    l.count -= n;
                }
        }
    }

    /** Mu held inside {@code stack}, 0 for anything but a loaded rack. */
    public static float mass(ItemStack stack) {
        CompoundTag tag = stack.get(FDataComponents.RACK_STOCK.get());
        return tag == null ? 0f : tag.getFloat(MASS);
    }
}
