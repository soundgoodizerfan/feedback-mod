// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** One line: the preheat coil's target temperature, or that it is off. */
public class PreheatComponent implements IBlockComponentProvider {

    public static final PreheatComponent INSTANCE = new PreheatComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(PreheatServerData.TARGET))
            return;
        int target = data.getInt(PreheatServerData.TARGET);
        tooltip.add((target <= 0 ? Component.translatable("message.feedback.preheat_off")
                : Component.translatable("message.feedback.preheat_target", target)).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("preheat");
    }
}
