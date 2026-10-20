/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.Feedback;
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
