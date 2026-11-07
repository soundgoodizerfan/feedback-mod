// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.logistics.belt.BeltBlockEntity;
import io.github.soundgoodizerfan.feedback.logistics.belt.ModuleHost;
import io.github.soundgoodizerfan.feedback.logistics.belt.BeltRuns;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** What a belt cell says about itself: its module, whether the module needs wiping, the counter's reading and the run's load. */
public class BeltServerData implements IServerDataProvider<BlockAccessor> {

    public static final BeltServerData INSTANCE = new BeltServerData();

    static final String MODULE = "BeltModule", SOILED = "BeltSoiled", COUNT = "BeltCount", RATE = "BeltRate", ENGAGED = "BeltEngaged", MASS = "BeltMass";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof ModuleHost belt))
            return;
        data.putBoolean("Belt", true);
        if (belt.module() != null) {
            data.putInt(MODULE, belt.module().ordinal());
            data.putBoolean(SOILED, belt instanceof BeltBlockEntity b && b.soiled());
            data.putBoolean(ENGAGED, belt.engaged());
            if (belt instanceof BeltBlockEntity b) {
                data.putLong(COUNT, b.getCount());
                data.putFloat(RATE, b.getRate());
            }
        }
        var run = BeltRuns.of(accessor.getLevel()).runAt(belt.getBlockPos());
        data.putFloat(MASS, run == null ? 0f : run.mass());
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("belt");
    }
}
