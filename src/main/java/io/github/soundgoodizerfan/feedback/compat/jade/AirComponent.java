// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Gas cell, ballast and propeller state in words; the figures belong to instruments. */
public class AirComponent implements IBlockComponentProvider {

    public static final AirComponent INSTANCE = new AirComponent();

    private static String fill(float f) {
        return f <= 0.001f ? "empty" : f < 0.34f ? "low" : f < 0.75f ? "half" : f < 0.98f ? "full" : "taut";
    }

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(AirServerData.KIND))
            return;
        switch (data.getString(AirServerData.KIND)) {
            case "cell" -> {
                tooltip.add(Component.translatable("jade.feedback.air.fill." + fill(data.getFloat(AirServerData.FILL))).withStyle(ChatFormatting.GRAY));
                if (data.contains(AirServerData.GAS)) {
                    ResourceLocation id = ResourceLocation.parse(data.getString(AirServerData.GAS));
                    tooltip.add(Component.translatable("fluid." + id.getNamespace() + "." + id.getPath()).withStyle(ChatFormatting.GRAY));
                }
                if (data.getBoolean(AirServerData.LEAK))
                    tooltip.add(Component.translatable("jade.feedback.air.leaking").withStyle(ChatFormatting.RED));
                if (data.getBoolean(AirServerData.BURNING))
                    tooltip.add(Component.translatable("jade.feedback.air.burning").withStyle(ChatFormatting.GOLD));
            }
            case "ballast" -> tooltip.add(Component.translatable("jade.feedback.air.ballast." + fill(data.getFloat(AirServerData.FILL)))
                    .withStyle(ChatFormatting.GRAY));
            case "prop" -> {
                tooltip.add(Component.translatable("jade.feedback.air.prop." + data.getInt(AirServerData.STATUS)).withStyle(ChatFormatting.GRAY));
                if (data.getBoolean(AirServerData.WORN))
                    tooltip.add(Component.translatable("jade.feedback.air.prop.worn").withStyle(ChatFormatting.YELLOW));
            }
            default -> { }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("air");
    }
}
