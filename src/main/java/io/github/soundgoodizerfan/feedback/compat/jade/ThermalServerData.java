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
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.machine.crucible.CrucibleBlockEntity;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Sends the truth about a thermal block to the client so the HUD can describe it.
 *
 * <h2>Same argument as {@link RotationServerData}</h2>
 * A crucible syncs its temperature only when its contents change, because syncing a float sixty
 * times a second to every nearby client so a HUD can round it to a word would be absurd. That
 * makes the client's copy stale by up to a whole process, which adjectives would survive and the
 * debug helmet would not -- and §8 permits an instrument to be wrong but never to overstate its
 * certainty. So the live figure is fetched on demand for the one block being looked at.
 * <p>
 * Note the {@code READABLE} flag. A sealed vessel refuses to be read at all, and the HUD must
 * distinguish <em>this is not measurable</em> from <em>this is cold</em>. That distinction is the
 * wall the whole beat is built around, so it would be a poor thing to lose in a tooltip.
 */
public class ThermalServerData implements IServerDataProvider<BlockAccessor> {

    public static final ThermalServerData INSTANCE = new ThermalServerData();

    static final String THERMAL = "Thermal";
    static final String TEMPERATURE = "Temperature";
    static final String READABLE = "Readable";
    static final String HEATING_RATE = "HeatingRate";
    static final String INSULATION = "Insulation";
    static final String FIRE_TU = "FireTu";
    static final String FLUIDS = "FluidHeat";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity be = accessor.getBlockEntity();

        if (be instanceof ThermalBody body) {
            data.putBoolean(THERMAL, true);
            data.putBoolean(READABLE, body.hasThermowell());
            data.putFloat(TEMPERATURE, body.getTemperature().value());
        }

        if (be instanceof CrucibleBlockEntity crucible) {
            data.putFloat(HEATING_RATE, crucible.getHeatingRate().tuPerTick());
            data.putInt(INSULATION, crucible.getInsulation());
        }

        if (be instanceof io.github.soundgoodizerfan.feedback.core.thermal.HeatedTanks tanks) {
            var list = new net.minecraft.nbt.ListTag();
            for (var tank : tanks.heatedTanks())
                if (!tank.isEmpty()) {
                    CompoundTag entry = new CompoundTag();
                    entry.putString("Fluid", tank.getFluid().getFluidType().getDescriptionId());
                    entry.putFloat("Tu", tank.temperature().value());
                    list.add(entry);
                }
            data.put(FLUIDS, list);
        }

        if (be instanceof io.github.soundgoodizerfan.feedback.machine.mixer.MixerBlockEntity mixer) {
            var list = new net.minecraft.nbt.ListTag();
            for (var c : mixer.contents()) {
                CompoundTag entry = new CompoundTag();
                entry.putString("Material", c.material().toString());
                entry.putInt("Count", c.count());
                list.add(entry);
            }
            data.put("MixerContents", list);
            data.putFloat("MixerUniformity", mixer.uniformity());
            data.putFloat("MixerHeat", mixer.heat());
            data.putBoolean("MixerOver", mixer.overmixed());
        }

        if (be instanceof io.github.soundgoodizerfan.feedback.vehicle.transmission.TransmissionBlockEntity box && box.design() != null)
            data.putString("Gear", box.label().getString());

        if (be instanceof io.github.soundgoodizerfan.feedback.machine.campfire.FeedbackCampfireBlockEntity fire)
            data.putBoolean("Buried", fire.buried());

        if (be instanceof HeatSource source)
            data.putFloat(FIRE_TU, source.getFireTu().value());
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("thermal");
    }
}
