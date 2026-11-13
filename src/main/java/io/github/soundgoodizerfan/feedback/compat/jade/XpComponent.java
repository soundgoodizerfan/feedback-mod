// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.tooltip.Readout;
import io.github.soundgoodizerfan.feedback.core.xp.XpStore;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * What an XP holder or wire shows: the seated container in words (level, fill), and whether the run is moving; with
 * an XP meter carried, the figures instead, signed by the meter. Words are free, numbers are the instrument's.
 */
public class XpComponent implements IBlockComponentProvider {

    public static final XpComponent INSTANCE = new XpComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.getBoolean(XpServerData.XP))
            return;
        boolean figures = Readout.instrumented(Quantity.XP_LEVEL);
        if (data.contains(XpServerData.STAGE)) {
            var stage = io.github.soundgoodizerfan.feedback.core.xp.Culture.Stage.values()[data.getInt(XpServerData.STAGE)];
            tooltip.add(Component.translatable("feedback.culture.stage." + stage.name().toLowerCase(java.util.Locale.ROOT))
                    .withStyle(ChatFormatting.GRAY));
            if (figures && stage != io.github.soundgoodizerfan.feedback.core.xp.Culture.Stage.EMPTY) {
                tooltip.add(Component.translatable("feedback.culture.mass", Readout.figure(Quantity.FIGURE, Math.round(data.getFloat(XpServerData.MASS) * 100)))
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("feedback.culture.air", Readout.figure(Quantity.FIGURE, data.getInt(XpServerData.AIR)))
                        .withStyle(ChatFormatting.GRAY));
            }
        }
        if (data.contains(XpServerData.MAX)) {
            XpStore s = new XpStore(data.getInt(XpServerData.POINTS), data.getFloat(XpServerData.PER_LEVEL),
                    data.getInt(XpServerData.MAX));
            tooltip.add(figures ? Readout.xpFigures(s.level(), s.points(), s.maxPoints())
                    : Component.translatable("tooltip.feedback.xp.held", Readout.xpLevel(s), Readout.xpFill(s))
                            .withStyle(ChatFormatting.GRAY));
        }
        if (data.contains(XpServerData.RUN_LEVEL)) {
            int flow = data.getInt(XpServerData.FLOW);
            tooltip.add(figures
                    ? Readout.reading(Quantity.XP_LEVEL, "feedback.readout.xp_run",
                            Readout.figure(Quantity.XP_LEVEL, data.getFloat(XpServerData.RUN_LEVEL)),
                            Readout.figure(Quantity.EXPERIENCE, data.getInt(XpServerData.RUN_POINTS)), flow,
                            data.getInt(XpServerData.LEAK))
                    : Component.translatable(flow > 0 ? "feedback.jade.xp.flowing" : "feedback.jade.xp.settled")
                            .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("xp");
    }
}
