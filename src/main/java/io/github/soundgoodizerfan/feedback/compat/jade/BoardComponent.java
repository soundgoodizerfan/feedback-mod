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
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** A job board in words: how many robots it hears, and each slot that is not free. */
public class BoardComponent implements IBlockComponentProvider {

    public static final BoardComponent INSTANCE = new BoardComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(BoardServerData.STATES))
            return;
        tooltip.add(Component.translatable("jade.feedback.board.hears", data.getInt(BoardServerData.HEARD), data.getInt(BoardServerData.RANGE))
                .withStyle(ChatFormatting.GRAY));
        int[] states = data.getIntArray(BoardServerData.STATES);
        var claimers = data.getList(BoardServerData.CLAIMERS, Tag.TAG_STRING);
        for (int i = 0; i < states.length; i++)
            if (states[i] == 1)
                tooltip.add(Component.translatable("jade.feedback.board.claimed", i, claimers.getString(i)).withStyle(ChatFormatting.GRAY));
            else if (states[i] == 2)
                tooltip.add(Component.translatable("jade.feedback.board.done", i).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("robot_board");
    }
}
