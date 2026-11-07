// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** One line per electrode slot, numbered 1 to 3 so a gap reads as a gap: material and condition, or empty. */
public class ArcComponent implements IBlockComponentProvider {

    public static final ArcComponent INSTANCE = new ArcComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(ArcServerData.ELECTRODES))
            return;
        ListTag list = data.getList(ArcServerData.ELECTRODES, net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (!entry.contains("Material")) {
                tooltip.add(Component.translatable("feedback.jade.arc.empty", i + 1).withStyle(ChatFormatting.DARK_GRAY));
                continue;
            }
            ResourceLocation id = ResourceLocation.parse(entry.getString("Material"));
            tooltip.add(Component.translatable("feedback.jade.arc.electrode", i + 1,
                    Component.translatable("material." + id.getNamespace() + "." + id.getPath()),
                    Math.round(entry.getFloat("Condition") * 100f)).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("arc");
    }
}
