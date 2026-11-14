// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.machine.glow.GlowGlassBlock;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** How a radiation-glowing glass block is glowing, in words: it needs no server data, the glow is its block state. */
public class GlowGlassComponent implements IBlockComponentProvider {

    public static final GlowGlassComponent INSTANCE = new GlowGlassComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockState().getBlock() instanceof GlowGlassBlock))
            return;
        int glow = accessor.getBlockState().getValue(GlowGlassBlock.GLOW);
        tooltip.add(Component.translatable(glow == 0 ? "feedback.jade.glow.dark" : glow <= 3 ? "feedback.jade.glow.faint"
                : glow <= 6 ? "feedback.jade.glow.glowing" : "feedback.jade.glow.bright"));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("glow_glass");
    }
}
