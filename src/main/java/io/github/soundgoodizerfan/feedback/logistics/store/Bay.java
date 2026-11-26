// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/** One pallet position of a rack: its fitting and what is in it. */
public final class Bay {

    public Fitting fitting = Fitting.PALLET;
    /** A cold plate or sealed liner, or {@code PALLET} for none. */
    public Fitting treatment = Fitting.PALLET;
    public final List<Lot> lots = new ArrayList<>(1);

    public boolean empty() {
        return lots.isEmpty();
    }

    private int pieces() {
        int n = 0;
        for (Lot l : lots)
            n += l.count;
        return n;
    }

    /**
     * Brings every lot up to now in surroundings of {@code envTu} (rot, and a heat stamp gone cold), then joins lots that have
     * come to match: a hot ingot merges with its cool kin only once their states agree. Returns whether anything changed.
     */
    public boolean settle(net.minecraft.world.level.Level level, float envTu) {
        boolean changed = false;
        for (Lot l : lots)
            changed |= l.advance(level, envTu);
        for (int i = 0; i < lots.size(); i++)
            for (int j = lots.size() - 1; j > i; j--)
                if (lots.get(i).matches(lots.get(j).item)) {
                    Lot into = lots.get(i), gone = lots.remove(j);
                    into.count += gone.count;
                    into.marks = Marks.merge(into.marks, gone.marks);
                    changed = true;
                }
        return changed;
    }

    public float mass() {
        float m = 0f;
        for (Lot l : lots)
            m += l.mass();
        return m;
    }

    /** How many of {@code stack} this bay would still take on a shelf rated {@code rating} (1 = the reference shelf). */
    public int room(ItemStack stack, float rating) {
        float unit = Goods.unitMass(stack);
        float cap = switch (fitting) {
            case BIN -> StoreTuning.BIN_MASS_MU;
            case CABINET -> StoreTuning.CABINET_MASS_MU;
            default -> StoreTuning.BAY_MASS_MU;
        } * rating;
        int byMass = unit <= 0f ? Integer.MAX_VALUE : Math.max(0, (int) ((cap - mass()) / unit + 1e-4f));
        Lot same = find(stack);
        return switch (fitting) {
            case PALLET -> same == null && !empty() ? 0
                    : Math.min(byMass, Math.max(0, StoreTuning.BAY_MAX_ITEMS - (same == null ? 0 : same.count)));
            case HOPPER -> !Goods.loose(stack) || same == null && !empty() ? 0 : byMass;
            case BIN -> unit > StoreTuning.BIN_UNIT_MAX_MU || same == null && lots.size() >= StoreTuning.BIN_KINDS ? 0 : byMass;
            case CABINET -> Math.min(byMass, Math.max(0, StoreTuning.CABINET_PIECES - pieces()));
            default -> 0;
        };
    }

    @org.jetbrains.annotations.Nullable
    public Lot find(ItemStack stack) {
        for (Lot l : lots)
            if (l.matches(stack))
                return l;
        return null;
    }

    /** The lot of these goods, allowing that settling has moved their rot or heat on since {@code stack} was seen ({@link Lot#like}). */
    @org.jetbrains.annotations.Nullable
    public Lot findLike(ItemStack stack) {
        Lot exact = find(stack);
        if (exact != null)
            return exact;
        for (Lot l : lots)
            if (l.like(stack))
                return l;
        return null;
    }

    /** Puts {@code n} of {@code stack} in with the bands recorded for them; the caller has asked {@link #room}. */
    public void add(ItemStack stack, int n, int marks) {
        Lot same = find(stack);
        if (same == null) {
            same = new Lot(stack, 0);
            lots.add(same);
        }
        same.count += n;
        same.marks = Marks.merge(same.marks, marks);
    }

    /** Takes up to {@code n} of {@code stack} (or its settled successor), returning how many left. */
    public int take(ItemStack stack, int n) {
        Lot same = findLike(stack);
        if (same == null)
            return 0;
        int out = Math.min(n, same.count);
        same.count -= out;
        if (same.count <= 0)
            lots.remove(same);
        return out;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Fitting", fitting.ordinal());
        tag.putInt("Treatment", treatment.ordinal());
        ListTag list = new ListTag();
        for (Lot l : lots)
            list.add(l.save(registries));
        tag.put("Lots", list);
        return tag;
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        fitting = Fitting.byOrdinal(tag.getInt("Fitting"));
        treatment = Fitting.byOrdinal(tag.getInt("Treatment"));
        lots.clear();
        ListTag list = tag.getList("Lots", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Lot l = Lot.load(list.getCompound(i), registries);
            if (l != null)
                lots.add(l);
        }
    }
}
