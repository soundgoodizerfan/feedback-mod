// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.item.MassProbeItem;
import io.github.soundgoodizerfan.feedback.machine.electrolytic.ElectrolyticCellBlockEntity;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * What a mass probe hung in an electrolytic cell shows (mechanics 6.17). The reading is taken here
 * on the server, where the coat is true, through the probe's own range, step and noise; the client
 * only prints it.
 */
public class ProbeServerData implements IServerDataProvider<BlockAccessor> {

    public static final ProbeServerData INSTANCE = new ProbeServerData();

    static final String PROBE = "ProbeItem", STACK = "ProbeStack", SHOWN = "ProbeShown";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof ElectrolyticCellBlockEntity cell
                && cell.getWork().getItem() instanceof MassProbeItem probe) {
            data.put(STACK, cell.getWork().save(accessor.getLevel().registryAccess()));
            data.putString(PROBE, BuiltInRegistries.ITEM.getKey(probe).toString());
            data.putFloat(SHOWN, probe.read(cell.getWork(), cell.coatTotal(), accessor.getLevel().random));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("probe");
    }
}
