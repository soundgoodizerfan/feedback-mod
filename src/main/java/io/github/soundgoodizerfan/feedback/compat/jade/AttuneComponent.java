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

/** Whether an attuned machine's reservoir feeds it: words only; the level figure belongs to the XP meter. */
public class AttuneComponent implements IBlockComponentProvider {

    public static final AttuneComponent INSTANCE = new AttuneComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        var data = accessor.getServerData();
        if (data.contains(AttuneServerData.ECHO))
            tooltip.add(Component.translatable("jade.feedback.echo." + data.getInt(AttuneServerData.ECHO))
                    .withStyle(data.getInt(AttuneServerData.ECHO) == 1 ? ChatFormatting.YELLOW : ChatFormatting.RED));
        if (data.contains(AttuneServerData.DRAW))
            tooltip.add(Component.translatable("jade.feedback.attune." + data.getInt(AttuneServerData.DRAW))
                    .withStyle(data.getInt(AttuneServerData.DRAW) == 0 ? ChatFormatting.GREEN : ChatFormatting.GRAY));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("attune");
    }
}
