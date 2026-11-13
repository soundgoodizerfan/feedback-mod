// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

/** One line per needle a machine's dowse part carries: what it seeks and the compass direction it leans, or that nothing is near. */
public class DowseComponent implements IBlockComponentProvider {

    public static final DowseComponent INSTANCE = new DowseComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(DowseServerData.NEEDLES))
            return;
        ListTag list = data.getList(DowseServerData.NEEDLES, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            int dir = e.getInt("Dir");
            Component where = dir == -1 ? Component.translatable("feedback.jade.dowse.none")
                    : dir == -2 ? Component.translatable("feedback.dowse.here") : Component.translatable("feedback.jade.dowse.dir." + dir);
            tooltip.add(Component.translatable(e.getBoolean("Away") ? "feedback.jade.dowse.away" : "feedback.jade.dowse.point",
                    Component.translatable("feedback.jade.dowse.prop." + e.getString("Property")), where).withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("dowse");
    }
}
