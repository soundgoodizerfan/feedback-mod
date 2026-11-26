// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.control.data.DataWires;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** How long a good data line at this node stays dark after quenching: the quench memory lives on the server only, so it is sent. */
public class DataDarkServerData implements IServerDataProvider<BlockAccessor> {

    public static final DataDarkServerData INSTANCE = new DataDarkServerData();

    static final String DARK = "DataDark";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof DataNode node && node.getNodeLevel() != null) {
            int ticks = DataWires.darkTicks(node.getNodeLevel(), node);
            if (ticks > 0)
                data.putInt(DARK, ticks);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("data_dark");
    }
}
