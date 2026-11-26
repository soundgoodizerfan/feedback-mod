// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.logistics.store.StoreTuning;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** A rack's bays in use and whether its shelves are worn; a dock's state in the words its screen uses. */
public class StoreComponent implements IBlockComponentProvider {

    public static final StoreComponent INSTANCE = new StoreComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data.contains(StoreServerData.BAYS)) {
            tooltip.add(Component.translatable("jade.feedback.rack.bays", data.getInt(StoreServerData.BAYS), StoreTuning.BAYS_PER_CELL)
                    .withStyle(ChatFormatting.GRAY));
            if (data.getBoolean(StoreServerData.WORN))
                tooltip.add(Component.translatable("jade.feedback.rack.worn").withStyle(ChatFormatting.YELLOW));
        }
        if (data.contains(StoreServerData.FILTER))
            tooltip.add(Component.translatable("message.feedback.rack.filter",
                    Component.translatable("feedback.sort.band." + data.getString(StoreServerData.FILTER))).withStyle(ChatFormatting.GRAY));
        if (data.getBoolean(StoreServerData.FOULED))
            tooltip.add(Component.translatable("jade.feedback.dock.fouled").withStyle(ChatFormatting.YELLOW));
        if (data.contains(StoreServerData.STATE))
            tooltip.add(Component.translatable("screen.feedback.dock.state." + data.getString(StoreServerData.STATE))
                    .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("warehouse");
    }
}
