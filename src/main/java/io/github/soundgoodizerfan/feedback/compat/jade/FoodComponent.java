// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.compat.jade;

import io.github.soundgoodizerfan.feedback.machine.column.PetrochemTuning;
import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.client.Readout;
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
                tooltip.add(Component.translatable("feedback.jade.canning.jars", jars[0] + jars[1], PetrochemTuning.CANNING_SLOTS)
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
        if (data.contains(FoodServerData.PAN)) {
            tooltip.add(Readout.temperatureReading(new Tu(data.getFloat(FoodServerData.PAN))));
            var list = data.getList(FoodServerData.PIECES, net.minecraft.nbt.Tag.TAG_FLOAT);
            boolean exact = Instruments.exact(accessor.getPlayer(), Quantity.FIGURE);
            for (int i = 0; i + 5 < list.size(); i += 6) {
                if (exact)
                    tooltip.add(Component.translatable("feedback.jade.skillet.piece", Math.round(list.getFloat(i)), Math.round(list.getFloat(i + 1) * 100f),
                            Math.round(list.getFloat(i + 2) * 100f), Math.round(list.getFloat(i + 3) * 100f), Math.round(list.getFloat(i + 4) * 100f))
                            .withStyle(ChatFormatting.GRAY));
                else
                    tooltip.add(Component.translatable("feedback.jade.skillet.piece_word", Component.translatable("tooltip.feedback.food."
                            + io.github.soundgoodizerfan.feedback.process.Cooking.Doneness.values()[(int) list.getFloat(i + 5)].name().toLowerCase(java.util.Locale.ROOT)))
                            .withStyle(ChatFormatting.GRAY));
            }
        }
        if (data.contains(FoodServerData.POT)) {
            tooltip.add(Readout.temperatureReading(new Tu(data.getFloat(FoodServerData.POT))));
            var fig = data.getList(FoodServerData.POT_FIG, net.minecraft.nbt.Tag.TAG_FLOAT);
            if (fig.size() >= 5 && Instruments.exact(accessor.getPlayer(), Quantity.FIGURE))
                tooltip.add(Component.translatable("feedback.jade.stockpot.figures", Math.round(fig.getFloat(0) * 100f), Math.round(fig.getFloat(1) * 100f),
                        Math.round(fig.getFloat(2) * 100f), Math.round(fig.getFloat(3)), Math.round(fig.getFloat(4))).withStyle(ChatFormatting.GRAY));
        }
        if (data.contains(FoodServerData.ROOM)) {
            var room = data.getList(FoodServerData.ROOM, net.minecraft.nbt.Tag.TAG_FLOAT);
            if (room.size() >= 5) {
                tooltip.add(Component.translatable("feedback.jade.fridge.room", Math.round(room.getFloat(0)), Math.round(room.getFloat(1))).withStyle(ChatFormatting.GRAY));
                tooltip.add(Readout.temperatureReading(new Tu(room.getFloat(2))));
                tooltip.add(Component.translatable("feedback.jade.fridge.leak", String.format(java.util.Locale.ROOT, "%.3f", room.getFloat(3))).withStyle(ChatFormatting.GRAY));
                if (room.getFloat(4) > 0f)
                    tooltip.add(Component.translatable("feedback.jade.fridge.staged").withStyle(ChatFormatting.AQUA));
            }
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
