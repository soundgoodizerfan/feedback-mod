// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * One round trip of the carriage: out to a bay, then home. A storing job carries its load out; a retrieval carries it home.
 */
public final class Job {

    public final boolean fetch;
    public final Aisle.Cell cell;
    public final int bay;
    /** What the trip is for (count one). */
    public final ItemStack item;
    /** How many a retrieval set out to bring. */
    public final int planned;
    /** The id of the request a retrieval serves, 0 for storing. */
    public int request;
    /** How many ride the carriage now. */
    public int load;
    public boolean returning;
    /** Cells run on the present leg. */
    public float progress;

    public Job(boolean fetch, Aisle.Cell cell, int bay, ItemStack item, int planned) {
        this.fetch = fetch;
        this.cell = cell;
        this.bay = bay;
        this.item = item.copyWithCount(1);
        this.planned = planned;
    }

    public float loadMass() {
        return load * Goods.unitMass(item);
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("Fetch", fetch);
        tag.putLong("Pos", cell.pos().asLong());
        tag.putInt("Rail", cell.rail());
        tag.putInt("Height", cell.height());
        tag.putInt("Side", cell.side());
        tag.putInt("Bay", bay);
        tag.put("Item", item.save(registries));
        tag.putInt("Planned", planned);
        tag.putInt("Load", load);
        tag.putInt("Request", request);
        tag.putBoolean("Returning", returning);
        tag.putFloat("Progress", progress);
        return tag;
    }

    @org.jetbrains.annotations.Nullable
    public static Job load(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStack item = ItemStack.parseOptional(registries, tag.getCompound("Item"));
        if (item.isEmpty())
            return null;
        Aisle.Cell cell = new Aisle.Cell(BlockPos.of(tag.getLong("Pos")), tag.getInt("Rail"), tag.getInt("Height"), tag.getInt("Side"));
        Job job = new Job(tag.getBoolean("Fetch"), cell, tag.getInt("Bay"), item, tag.getInt("Planned"));
        job.load = tag.getInt("Load");
        job.request = tag.getInt("Request");
        job.returning = tag.getBoolean("Returning");
        job.progress = tag.getFloat("Progress");
        return job;
    }
}
