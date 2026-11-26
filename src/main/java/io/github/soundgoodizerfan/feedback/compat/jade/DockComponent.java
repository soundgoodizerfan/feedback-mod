// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** A robot dock or reel in words: free for a robot, or occupied (or unpowered). */
public class DockComponent implements IBlockComponentProvider {

    public static final DockComponent INSTANCE = new DockComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var data = accessor.getServerData();
        if (data.getBoolean(DockServerData.DOCK))
            tooltip.add(Component.translatable(data.getBoolean(DockServerData.FREE) ? "jade.feedback.dock.free" : "jade.feedback.dock.occupied")
                    .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("robot_dock");
    }
}
