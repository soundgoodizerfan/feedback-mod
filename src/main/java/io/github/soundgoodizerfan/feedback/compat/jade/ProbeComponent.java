// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.item.MassProbeItem;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** The hung mass probe's reading, signed by the probe: the instrument is the thing in the water. */
public class ProbeComponent implements IBlockComponentProvider {

    public static final ProbeComponent INSTANCE = new ProbeComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data.contains(ProbeServerData.PROBE)
                && BuiltInRegistries.ITEM.get(ResourceLocation.parse(data.getString(ProbeServerData.PROBE))) instanceof MassProbeItem probe)
            tooltip.add(probe.signed(ItemStack.parseOptional(accessor.getLevel().registryAccess(),
                    data.getCompound(ProbeServerData.STACK)), data.getFloat(ProbeServerData.SHOWN)));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("probe");
    }
}
