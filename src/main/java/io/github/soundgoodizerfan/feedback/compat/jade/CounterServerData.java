// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.village.CounterBlockEntity;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** A Counter's catchment figures: villagers in reach, who wants what the shelf holds, errands, who cannot get there, today's sales. */
public class CounterServerData implements IServerDataProvider<BlockAccessor> {

    public static final CounterServerData INSTANCE = new CounterServerData();

    static final String READOUT = "CounterReadout";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof CounterBlockEntity counter)
            data.put(READOUT, counter.readout());
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("counter");
    }
}
