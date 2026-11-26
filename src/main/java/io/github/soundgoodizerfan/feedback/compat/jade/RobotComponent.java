// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;

import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** A robot's state in words: what it is doing, how full its battery is, whether it has a program or is tied to a reel. Words are free. */
public class RobotComponent implements IEntityComponentProvider {

    public static final RobotComponent INSTANCE = new RobotComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(RobotServerData.CHARGE))
            return;
        if (data.contains(RobotServerData.DOING))
            tooltip.add(Component.translatable(data.getString(RobotServerData.DOING)).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(data.getBoolean(RobotServerData.BATTERY) ? "jade.feedback.robot.charge." + data.getInt(RobotServerData.CHARGE)
                : "jade.feedback.robot.no_battery").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(data.getBoolean(RobotServerData.PROGRAM) ? "robot.feedback.program" : "robot.feedback.no_program")
                .withStyle(ChatFormatting.GRAY));
        if (data.getInt(RobotServerData.SLOW) > 1)
            tooltip.add(Component.translatable("robot.feedback.slowed", data.getInt(RobotServerData.SLOW)).withStyle(ChatFormatting.GRAY));
        if (data.getBoolean(RobotServerData.TETHER))
            tooltip.add(Component.translatable("jade.feedback.robot.tethered").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("robot");
    }
}
