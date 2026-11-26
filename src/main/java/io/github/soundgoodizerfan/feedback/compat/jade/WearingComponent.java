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
        if (accessor.getServerData().getBoolean(WearingServerData.JAMMED))
            tooltip.add(Readout.gloss(Component.translatable("feedback.readout.jammed").withStyle(net.minecraft.ChatFormatting.GOLD), "feedback.gloss.jammed"));
        if (accessor.getServerData().contains(WearingServerData.WATER)) {
            float water = accessor.getServerData().getFloat(WearingServerData.WATER);
            tooltip.add(Component.translatable(water <= 0f ? "feedback.readout.jet_water.dry" : water < 0.25f ? "feedback.readout.jet_water.low"
                    : "feedback.readout.jet_water.full").withStyle(water <= 0f ? net.minecraft.ChatFormatting.GOLD : net.minecraft.ChatFormatting.GRAY));
        }
        if (accessor.getServerData().contains(WearingServerData.PLUG)) {
            Component plug = Readout.conditionReading(accessor.getServerData().getFloat(WearingServerData.PLUG));
            if (plug != null)
                tooltip.add(Component.translatable("feedback.readout.plug", plug));
        }
        if (accessor.getServerData().contains(WearingServerData.DAMAGE)) {
            // The engine's other wear: what knock and heat took, against the plug's wear from running time above.
            Component damage = Readout.conditionReading(accessor.getServerData().getFloat(WearingServerData.DAMAGE));
            if (damage != null)
                tooltip.add(Component.translatable("feedback.readout.engine_damage", damage));
        }
        if (accessor.getServerData().getBoolean(WearingServerData.SEIZED))
            tooltip.add(Readout.gloss(Component.translatable("feedback.readout.seized").withStyle(net.minecraft.ChatFormatting.GOLD), "feedback.gloss.seized"));
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
