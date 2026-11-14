// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** A hydride bed's state by words; the amount of gas needs a gauge. */
public class HydrideComponent implements IBlockComponentProvider {

    public static final HydrideComponent INSTANCE = new HydrideComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag t = accessor.getServerData().getCompound(HydrideServerData.BED);
        if (t.isEmpty())
            return;
        float fill = t.getFloat("Fill");
        tooltip.add(Component.translatable(t.getInt("Ingots") == 0 ? "feedback.jade.hydride.empty"
                : t.getBoolean("Giving") ? "feedback.jade.hydride.giving"
                : fill >= 0.9f ? "feedback.jade.hydride.full" : fill > 0f ? "feedback.jade.hydride.filling" : "feedback.jade.hydride.dry"));
        if (t.getFloat("Condition") < 0.95f)
            tooltip.add(Component.translatable("feedback.jade.hydride.worn"));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("hydride");
    }
}
