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

/** A levitation plate's state (held off or resting) and a sleeve's (cushioned or dry), by words; the field itself needs an instrument. */
public class LevitationComponent implements IBlockComponentProvider {

    public static final LevitationComponent INSTANCE = new LevitationComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data.contains(LevitationServerData.PLATE))
            tooltip.add(Component.translatable(data.getFloat(LevitationServerData.PLATE) > 0.05f
                    ? "feedback.jade.levitation.plate.held" : "feedback.jade.levitation.plate.resting"));
        if (data.contains(LevitationServerData.PYRO)) {
            float r = data.getFloat(LevitationServerData.PYRO);
            float fast = io.github.soundgoodizerfan.feedback.machine.pyro.PyroTuning.FAST_TU_PER_S;
            tooltip.add(Component.translatable(r == 0f ? "feedback.jade.pyro.still" : r >= fast ? "feedback.jade.pyro.warming_fast"
                    : r > 0f ? "feedback.jade.pyro.warming" : r <= -fast ? "feedback.jade.pyro.cooling_fast" : "feedback.jade.pyro.cooling"));
        }
        if (data.contains(LevitationServerData.SLEEVE))
            tooltip.add(Component.translatable(data.getFloat(LevitationServerData.SLEEVE) > 0.05f
                    ? "feedback.jade.levitation.sleeve.cushioned" : "feedback.jade.levitation.sleeve.dry"));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("levitation");
    }
}
