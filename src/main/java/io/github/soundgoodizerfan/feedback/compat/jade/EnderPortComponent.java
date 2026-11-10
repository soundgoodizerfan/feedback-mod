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

/** Pair state, distance, teleportant level and anchor of an ender port. */
public class EnderPortComponent implements IBlockComponentProvider {

    public static final EnderPortComponent INSTANCE = new EnderPortComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(EnderPortServerData.STATE))
            return;
        tooltip.add(Component.literal(data.getString(EnderPortServerData.STATE)).withStyle(ChatFormatting.GRAY));
        float distance = data.getFloat(EnderPortServerData.DISTANCE);
        if (distance >= 0f)
            tooltip.add(Component.translatable("message.feedback.ender_port.distance", Math.round(distance)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("message.feedback.ender_port.teleportant", data.getInt(EnderPortServerData.TELEPORTANT))
                .withStyle(data.getInt(EnderPortServerData.TELEPORTANT) <= 0 ? ChatFormatting.RED : ChatFormatting.GRAY));
        if (data.getBoolean(EnderPortServerData.ANCHORED))
            tooltip.add(Component.translatable("message.feedback.ender_port.chunk_anchored").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("ender_port");
    }
}
