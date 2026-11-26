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
    /** What the dock's heads recorded of these goods at intake ({@link Marks}). */
    public int marks;

    public Lot(ItemStack item, int count) {
        this.item = item.copyWithCount(1);
        this.count = count;
    }

    public boolean matches(ItemStack other) {
        return ItemStack.isSameItemSameComponents(item, other);
    }

    /**
     * Whether {@code other} is these goods under an older reading: the same but for the rot clock and heat stamp, which
     * settling rewrites. A request made against an earlier look at the stock still finds its lot.
     */
    public boolean like(ItemStack other) {
        if (matches(other))
            return true;
        ItemStack a = item.copy(), b = other.copy();
        for (ItemStack s : new ItemStack[] {a, b}) {
            s.remove(io.github.soundgoodizerfan.feedback.registry.FDataComponents.FOOD_STATE.get());
            s.remove(io.github.soundgoodizerfan.feedback.registry.FDataComponents.TEMPERATURE.get());
            s.remove(io.github.soundgoodizerfan.feedback.registry.FDataComponents.HEATED_AT.get());
        }
        return ItemStack.isSameItemSameComponents(a, b);
    }

    /**
     * Rot brought up to now in surroundings of {@code envTu} (the food's own stamp keeps its clock), and a heat stamp dropped
     * once the goods are cool. Returns whether the stack changed.
     */
    public boolean advance(net.minecraft.world.level.Level level, float envTu) {
        ItemStack before = item.copy();
        io.github.soundgoodizerfan.feedback.process.Cooking.settle(item, level, envTu);
        io.github.soundgoodizerfan.feedback.core.thermal.ItemHeat.settle(item, level);
        return !ItemStack.isSameItemSameComponents(before, item);
    }

    /** Whether these goods change with time: a heat stamp or a rot clock on them. */
    public boolean dynamic() {
        return item.has(io.github.soundgoodizerfan.feedback.registry.FDataComponents.TEMPERATURE.get())
                || item.has(io.github.soundgoodizerfan.feedback.registry.FDataComponents.FOOD_STATE.get());
    }

    public float mass() {
        return Goods.unitMass(item) * count;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.put("Item", item.save(registries));
        tag.putInt("Count", count);
        tag.putInt("Marks", marks);
        return tag;
    }

    /** Null for a tag whose item no longer exists. */
    @org.jetbrains.annotations.Nullable
    public static Lot load(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStack item = ItemStack.parseOptional(registries, tag.getCompound("Item"));
        int count = tag.getInt("Count");
        if (item.isEmpty() || count <= 0)
            return null;
        Lot lot = new Lot(item, count);
        lot.marks = tag.getInt("Marks");
        return lot;
    }
}
