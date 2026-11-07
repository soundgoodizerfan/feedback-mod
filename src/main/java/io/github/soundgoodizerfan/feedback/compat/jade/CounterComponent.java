// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The Counter's catchment readout: the player tunes the layout by what this says, not by guessing. Counts of things in the world, free to read. */
public class CounterComponent implements IBlockComponentProvider {

    public static final CounterComponent INSTANCE = new CounterComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag r = accessor.getServerData().getCompound(CounterServerData.READOUT);
        if (r.isEmpty() || !r.getBoolean("Stocked"))
            return;
        tooltip.add(Component.translatable("feedback.jade.counter.catchment", r.getInt("InReach"), r.getInt("Wanting")));
        tooltip.add(Component.translatable("feedback.jade.counter.errands", r.getInt("Errands")));
        if (r.getInt("Sealed") > 0)
            tooltip.add(Component.translatable("feedback.jade.counter.sealed", r.getInt("Sealed")));
        if (r.getInt("TooDear") > 0)
            tooltip.add(Component.translatable("feedback.jade.counter.too_dear", r.getInt("TooDear")));
        if (r.getInt("Stalled") > 0)
            tooltip.add(Component.translatable("feedback.jade.counter.stalled", r.getInt("Stalled")));
        if (r.getInt("Road") > 0)
            tooltip.add(Component.translatable("feedback.jade.counter.road", r.getInt("Road")));
        tooltip.add(Component.translatable("feedback.jade.counter.sales", r.getInt("WalkIn"), r.getInt("Hand")));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("counter");
    }
}
