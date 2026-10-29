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
import io.github.soundgoodizerfan.feedback.client.Readout;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.instrument.Instruments;
import io.github.soundgoodizerfan.feedback.instrument.Quantity;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * The keeping equipment's card. Heat goes through the instrument seam like any temperature; how many jars are sealed and
 * which is the Ice Box's cold source are things you can see or hear, so they are free. Sealing progress is a word until the
 * truth tier ({@link Quantity#FIGURE}), then a percentage.
 */
public class FoodComponent implements IBlockComponentProvider {

    public static final FoodComponent INSTANCE = new FoodComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();
        if (data.contains(FoodServerData.VESSEL)) {
            tooltip.add(Readout.temperatureReading(new Tu(data.getFloat(FoodServerData.VESSEL))));
            int[] jars = data.getIntArray(FoodServerData.JARS);
            if (jars.length == 3) {
                tooltip.add(Component.translatable("feedback.jade.canning.jars", jars[0] + jars[1], FTuning.CANNING_SLOTS)
                        .withStyle(ChatFormatting.GRAY));
                if (jars[0] > 0)
                    tooltip.add(Component.translatable("feedback.jade.canning.sealed", jars[0]).withStyle(ChatFormatting.GRAY));
                if (jars[2] > 0)
                    tooltip.add(Component.translatable("feedback.jade.canning.mushy", jars[2]).withStyle(ChatFormatting.GOLD));
            }
            float sealing = data.getFloat(FoodServerData.SEALING);
            if (sealing >= 0f)
                tooltip.add(Instruments.exact(accessor.getPlayer(), Quantity.FIGURE)
                        ? Component.translatable("feedback.jade.canning.sealing_pct", Math.round(sealing * 100f))
                        : Component.translatable(sealing < 0.34f ? "feedback.jade.canning.sealing_start"
                                : sealing < 0.67f ? "feedback.jade.canning.sealing_half" : "feedback.jade.canning.sealing_near"));
        }
        if (data.contains(FoodServerData.BOX)) {
            tooltip.add(Readout.temperatureReading(new Tu(data.getFloat(FoodServerData.BOX))));
            tooltip.add(Component.translatable("feedback.jade.icebox.hold." + data.getInt(FoodServerData.HOLD)).withStyle(ChatFormatting.GRAY));
        }
        if (data.contains(FoodServerData.PUMP))
            tooltip.add(Component.translatable(data.getBoolean(FoodServerData.PUMP) ? "feedback.jade.fridge.running" : "feedback.jade.fridge.idle")
                    .withStyle(ChatFormatting.GRAY));
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("food");
    }
}
