// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.hydride.HydrideBedBlockEntity;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

/** What a hydride bed holds, in words: nothing loaded, dry, soaking, nearly full, giving gas back, and whether it has cracked. */
public class HydrideServerData implements IServerDataProvider<BlockAccessor> {

    public static final HydrideServerData INSTANCE = new HydrideServerData();

    static final String BED = "HydrideBed";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof HydrideBedBlockEntity bed) {
            CompoundTag t = new CompoundTag();
            t.putInt("Ingots", bed.ingots().size());
            t.putFloat("Fill", bed.fill());
            t.putBoolean("Giving", bed.giving());
            t.putFloat("Condition", bed.condition());
            data.put(BED, t);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("hydride");
    }
}
