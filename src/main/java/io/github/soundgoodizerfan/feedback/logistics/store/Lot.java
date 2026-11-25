// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Many of one stack: a template (count one) and how many. Counts run past a stack's own limit. */
public final class Lot {

    public final ItemStack item;
    public int count;

    public Lot(ItemStack item, int count) {
        this.item = item.copyWithCount(1);
        this.count = count;
    }

    public boolean matches(ItemStack other) {
        return ItemStack.isSameItemSameComponents(item, other);
    }

    public float mass() {
        return Goods.unitMass(item) * count;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Item", item.save(registries));
        tag.putInt("Count", count);
        return tag;
    }

    /** Null for a tag whose item no longer exists. */
    @org.jetbrains.annotations.Nullable
    public static Lot load(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStack item = ItemStack.parseOptional(registries, tag.getCompound("Item"));
        int count = tag.getInt("Count");
        return item.isEmpty() || count <= 0 ? null : new Lot(item, count);
    }
}
