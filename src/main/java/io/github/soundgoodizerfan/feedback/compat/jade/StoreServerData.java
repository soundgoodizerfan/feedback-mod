// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import java.util.Locale;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.logistics.store.DockBlockEntity;
import io.github.soundgoodizerfan.feedback.logistics.store.RackBlockEntity;
import io.github.soundgoodizerfan.feedback.logistics.store.StoreTuning;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** What a warehouse rack or dock says about itself: bays in use and shelf wear, or what holds the carriage up. */
public class StoreServerData implements IServerDataProvider<BlockAccessor> {

    public static final StoreServerData INSTANCE = new StoreServerData();

    static final String BAYS = "StoreBays", WORN = "StoreWorn", STATE = "StoreState";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof RackBlockEntity rack) {
            int used = 0;
            for (int i = 0; i < StoreTuning.BAYS_PER_CELL; i++)
                if (!rack.bay(i).empty())
                    used++;
            data.putInt(BAYS, used);
            data.putBoolean(WORN, rack.condition() < 1f);
        } else if (accessor.getBlockEntity() instanceof DockBlockEntity dock) {
            data.putString(STATE, dock.state().name().toLowerCase(Locale.ROOT));
        } else if (accessor.getBlockEntity() instanceof io.github.soundgoodizerfan.feedback.logistics.store.DeskBlockEntity desk
                && desk.down() != null) {
            data.putString(STATE, desk.down().name().toLowerCase(Locale.ROOT));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("warehouse");
    }
}
