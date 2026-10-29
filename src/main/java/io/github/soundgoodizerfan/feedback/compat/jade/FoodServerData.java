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
import io.github.soundgoodizerfan.feedback.machine.icebox.IceBoxBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.pressurevessel.PressureVesselBlockEntity;
import io.github.soundgoodizerfan.feedback.machine.refrigeration.RefrigerationUnitBlockEntity;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * The keeping equipment's live state, fetched on demand for the one block looked at (as {@link ThermalServerData}): the Pressure
 * Vessel's jars and heat, the Ice Box's air and what is holding it cold, whether the Refrigeration Unit is pumping.
 */
public class FoodServerData implements IServerDataProvider<BlockAccessor> {

    public static final FoodServerData INSTANCE = new FoodServerData();

    static final String VESSEL = "CanningTu", JARS = "CanningJars", SEALING = "CanningSealing", BOX = "IceBoxTu", HOLD = "IceBoxHold", PUMP = "FridgeRunning";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (accessor.getBlockEntity() instanceof PressureVesselBlockEntity vessel) {
            data.putFloat(VESSEL, vessel.temperature());
            data.putIntArray(JARS, vessel.jarStates());
            data.putFloat(SEALING, vessel.leastSterile());
        }
        if (accessor.getBlockEntity() instanceof IceBoxBlockEntity box) {
            data.putFloat(BOX, box.boxTemperature());
            // 2 pumped, 1 melting ice, 0 neither: it is drifting to the room.
            data.putInt(HOLD, box.pumped() ? 2 : box.hasCold() ? 1 : 0);
        }
        if (accessor.getBlockEntity() instanceof RefrigerationUnitBlockEntity unit)
            data.putBoolean(PUMP, unit.running());
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("food");
    }
}
