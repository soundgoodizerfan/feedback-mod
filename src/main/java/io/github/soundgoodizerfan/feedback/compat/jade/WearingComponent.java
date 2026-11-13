// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.tooltip.Readout;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** The condition line for a worn block with no workpiece, from {@link WearingServerData}. */
public class WearingComponent implements IBlockComponentProvider {

    public static final WearingComponent INSTANCE = new WearingComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!accessor.getServerData().contains(WearingServerData.CONDITION))
            return;
        Component condition = Readout.conditionReading(accessor.getServerData().getFloat(WearingServerData.CONDITION));
        if (condition != null)
            tooltip.add(condition);
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("wearing");
    }
}
