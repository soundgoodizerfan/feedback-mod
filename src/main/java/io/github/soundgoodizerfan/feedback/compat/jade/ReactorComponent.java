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

/** The fission card: a core's power, decay heat, pressure, rods and flaws; a channel's burnup; an assembly's position. */
public class ReactorComponent implements IBlockComponentProvider {

    public static final ReactorComponent INSTANCE = new ReactorComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(ReactorServerData.KEY))
            return;
        CompoundTag in = data.getCompound(ReactorServerData.KEY);
        switch (in.getString("Kind")) {
            case "core" -> {
                tooltip.add(Component.translatable("feedback.jade.fission.power", Math.round(in.getFloat("Power") * 100f),
                        Math.round(in.getFloat("K") * 100f) / 100f));
                tooltip.add(Component.translatable("feedback.jade.fission.fuel", in.getInt("Fuel"),
                        Math.round(in.getFloat("Health") * 100f)));
                tooltip.add(Component.translatable("feedback.jade.fission.decay", Math.round(in.getFloat("Decay") * 10f) / 10f));
                tooltip.add(Component.translatable("feedback.jade.fission.pressure", Math.round(in.getFloat("Pressure") * 10f) / 10f));
                if (!in.getString("Rods").isEmpty())
                    tooltip.add(Component.translatable("feedback.jade.fission.rods", in.getString("Rods")));
                if (in.getBoolean("Scrammed"))
                    tooltip.add(Component.translatable("feedback.jade.fission.scram").withStyle(ChatFormatting.RED));
                if (!in.getBoolean("Formed"))
                    tooltip.add(Component.translatable("feedback.jade.fission.flaws", in.getString("Flaws")).withStyle(ChatFormatting.YELLOW));
            }
            case "channel" -> tooltip.add(Component.translatable(in.getBoolean("Live") ? "feedback.jade.fission.channel" : "feedback.jade.fission.spent",
                    Math.round(in.getFloat("Burn") * 100f), Math.round(in.getFloat("Condition") * 100f)));
            case "rods" -> tooltip.add(Component.translatable("feedback.jade.fission.assembly", in.getInt("Position"),
                    in.getInt("Target"), Math.round(in.getFloat("Strength") * 100f)));
            default -> {
            }
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("fission");
    }
}
