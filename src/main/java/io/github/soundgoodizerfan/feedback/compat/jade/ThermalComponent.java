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
import io.github.soundgoodizerfan.feedback.control.bimetallic.BimetallicStripBlockEntity;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.machine.crucible.CrucibleBlockEntity;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * How hot a vessel is, in the vocabulary the player has paid for.
 *
 * <h2>The rule, on a HUD</h2>
 * "Really hot" on a bare crucible; a figure once a thermometer is carried, at that instrument's
 * resolution. Nothing here decides the wording -- {@link Readout#temperatureReading} does, in the
 * one file where §8's rule is enforceable by reading it.
 * <p>
 * A sealed vessel says so explicitly rather than saying nothing, because the player needs to learn
 * that it <em>cannot</em> be measured, not that they have failed to look properly.
 *
 * <h2>What is free here and why</h2>
 * The strip's trip point and the crucible's insulation count are specs, printed on the part, and
 * exact. Temperature, hold progress and heating rate are state, and go through the instrument
 * seam. That is the same line the hammer's card draws.
 */
public class ThermalComponent implements IBlockComponentProvider {

    public static final ThermalComponent INSTANCE = new ThermalComponent();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        CompoundTag data = accessor.getServerData();

        if (data.getBoolean(ThermalServerData.THERMAL)) {
            if (!data.getBoolean(ThermalServerData.READABLE))
                tooltip.add(Component.translatable("feedback.readout.sealed")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            else
                tooltip.add(Readout.temperatureReading(
                        new Tu(data.getFloat(ThermalServerData.TEMPERATURE))));
        }

        // What is hot inside the tanks, read the same way (the instrument seam decides the wording).
        for (net.minecraft.nbt.Tag t : data.getList(ThermalServerData.FLUIDS, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) t;
            tooltip.add(Component.translatable(entry.getString("Fluid")).append(": ")
                    .append(Readout.temperatureReading(new Tu(entry.getFloat("Tu")))));
        }

        if (data.contains("Gear"))
            tooltip.add(Component.translatable("message.feedback.transmission.in", data.getString("Gear")));

        // The Mixer's drum is open: its load and how mixed it is are plain to see. Only the heat is state.
        if (accessor.getBlockEntity() instanceof io.github.soundgoodizerfan.feedback.machine.mixer.MixerBlockEntity) {
            var load = data.getList("MixerContents", net.minecraft.nbt.Tag.TAG_COMPOUND);
            if (load.isEmpty())
                tooltip.add(Component.translatable("feedback.mixer.empty").withStyle(ChatFormatting.GRAY));
            for (net.minecraft.nbt.Tag t : load) {
                CompoundTag entry = (CompoundTag) t;
                var id = ResourceLocation.parse(entry.getString("Material"));
                tooltip.add(Component.translatable("cartridge.feedback.part", entry.getInt("Count"),
                        Component.translatable("material." + id.getNamespace() + "." + id.getPath())));
            }
            if (!load.isEmpty()) {
                float u = data.getFloat("MixerUniformity");
                tooltip.add(Component.translatable(data.getBoolean("MixerOver") ? "feedback.mixer.over" : u < 0.34f ? "feedback.mixer.unmixed" : u < 0.9f ? "feedback.mixer.partly"
                        : u < 0.999f ? "feedback.mixer.settling" : "feedback.mixer.mixed"));
                if (data.getFloat("MixerHeat") > 0)
                    tooltip.add(Component.translatable("feedback.mixer.heat",
                            Readout.temperatureReading(new Tu(data.getFloat("MixerHeat")))).withStyle(ChatFormatting.RED));
            }
        }

        if (accessor.getBlockEntity() instanceof CrucibleBlockEntity crucible) {
            int insulation = data.getInt(ThermalServerData.INSULATION);
            if (insulation > 0)
                tooltip.add(Component.translatable("feedback.spec.insulation", insulation)
                        .withStyle(ChatFormatting.DARK_GRAY));
            for (ItemStack held : crucible.getContents())
                if (!held.isEmpty())
                    tooltip.add(held.getHoverName());
        }

        if (accessor.getBlockEntity() instanceof BimetallicStripBlockEntity strip) {
            // The trip point is stamped on the strip. It is the reason this strip and not another
            // one was crafted, so hiding it would hide the only thing that distinguishes them.
            tooltip.add(Component.translatable("feedback.spec.trip",
                    Readout.number(FTuning.BIMETALLIC_TRIP_TU.value())).withStyle(ChatFormatting.DARK_GRAY));
            tooltip.add(Component.translatable(strip.isTripped()
                            ? "feedback.readout.strip_open"
                            : "feedback.readout.strip_closed")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("thermal");
    }
}
