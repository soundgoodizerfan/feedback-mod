// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.Readout;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The frame's drive card. Bare-eyed it says only whether the frame is driven -- a thing you could
 * tell by watching the tubes turn; the debug helmet gives every figure the drivetrain holds.
 */
public class FrameComponent implements IBlockComponentProvider {

    public static final FrameComponent INSTANCE = new FrameComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (!data.contains(FrameServerData.LINES))
            return;
        if (data.contains(FrameServerData.PADS)) {
            Component pads = Readout.conditionReading(data.getFloat(FrameServerData.PADS));
            if (pads != null)
                tooltip.add(Component.translatable("feedback.jade.frame.pads", pads));
        }
        if (!Instruments.exact(accessor.getPlayer(), Quantity.FIGURE)) {
            tooltip.add(Component.translatable(data.getBoolean(FrameServerData.COUPLED)
                    ? "feedback.jade.frame.driven" : "feedback.jade.frame.not_driven"));
            return;
        }
        for (Tag line : data.getList(FrameServerData.LINES, Tag.TAG_STRING))
            tooltip.add(Component.literal(line.getAsString()));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("frame_drive");
    }
}
