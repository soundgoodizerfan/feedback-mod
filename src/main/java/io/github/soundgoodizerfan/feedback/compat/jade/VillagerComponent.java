// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.village.VillageTuning;

import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** A villager's standing with the viewer, in words: strangers up to honoured, or wary below zero. Words are free. */
public class VillagerComponent implements IEntityComponentProvider {

    public static final VillagerComponent INSTANCE = new VillagerComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(VillagerServerData.STANDING))
            return;
        if (data.contains(VillagerServerData.GUILD_SIZE))
            tooltip.add(Component.translatable("village.feedback.guild", Component.translatable("entity.minecraft.villager." + data.getString(VillagerServerData.GUILD_PROFESSION)),
                    data.getInt(VillagerServerData.GUILD_SIZE), data.getInt(VillagerServerData.GUILD_TIER)).withStyle(ChatFormatting.GOLD));
        int rep = data.getInt(VillagerServerData.STANDING);
        int tier = 0;
        for (int t : VillageTuning.STANDING_TIERS)
            if (rep >= t)
                tier++;
        tooltip.add(Component.translatable(rep < 0 ? "village.feedback.standing.wary" : "village.feedback.standing." + tier)
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("villager");
    }
}
