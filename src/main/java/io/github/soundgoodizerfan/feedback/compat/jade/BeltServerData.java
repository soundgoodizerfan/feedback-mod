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
import io.github.soundgoodizerfan.feedback.logistics.belt.BeltBlockEntity;
import io.github.soundgoodizerfan.feedback.logistics.belt.ModuleHost;
import io.github.soundgoodizerfan.feedback.logistics.belt.BeltRuns;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/** What a belt cell says about itself: its module, whether the module needs wiping, the counter's reading and the run's load. */
public class BeltServerData implements IServerDataProvider<BlockAccessor> {

    public static final BeltServerData INSTANCE = new BeltServerData();

    static final String MODULE = "BeltModule", SOILED = "BeltSoiled", COUNT = "BeltCount", RATE = "BeltRate", ENGAGED = "BeltEngaged", MASS = "BeltMass";

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        if (!(accessor.getBlockEntity() instanceof ModuleHost belt))
            return;
        data.putBoolean("Belt", true);
        if (belt.module() != null) {
            data.putInt(MODULE, belt.module().ordinal());
            data.putBoolean(SOILED, belt instanceof BeltBlockEntity b && b.soiled());
            data.putBoolean(ENGAGED, belt.engaged());
            if (belt instanceof BeltBlockEntity b) {
                data.putLong(COUNT, b.getCount());
                data.putFloat(RATE, b.getRate());
            }
        }
        var run = BeltRuns.of(accessor.getLevel()).runAt(belt.getBlockPos());
        data.putFloat(MASS, run == null ? 0f : run.mass());
    }

    @Override
    public ResourceLocation getUid() {
        return Feedback.id("belt");
    }
}
