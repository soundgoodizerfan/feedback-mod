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
package io.github.soundgoodizerfan.feedback.machine.steamturbine;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.machine.steamengine.SteamEngineBlockEntity;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A Steam Engine at factory scale: the same tank, pull, band and exhaust logic (a Condenser drains
 * the exhaust the same way) with the {@code FTuning.STEAM_TURBINE_*} figures, about 16 kW from
 * 100 mB/t of hot steam. It has no wear of its own: the engine has none either.
 */
public class SteamTurbineBlockEntity extends SteamEngineBlockEntity {

    public SteamTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.STEAM_TURBINE.get(), pos, state, FTuning.STEAM_TURBINE_TANK_MB);
    }

    @Override protected float minTu() { return FTuning.STEAM_TURBINE_MIN_TU; }
    @Override protected float bandTu() { return FTuning.STEAM_TURBINE_BAND_TU; }
    @Override protected int bands() { return FTuning.STEAM_TURBINE_BANDS; }
    @Override protected int mbPerTick() { return FTuning.STEAM_TURBINE_MB_PER_TICK; }
    @Override protected float exhaustDropTu() { return FTuning.STEAM_TURBINE_EXHAUST_DROP_TU; }
    @Override protected Rpm workingRpm() { return FTuning.STEAM_TURBINE_RPM; }
    @Override protected Su fullCapacity() { return FTuning.STEAM_TURBINE_CAPACITY_SU; }
    @Override protected Inertia inertia() { return FTuning.STEAM_TURBINE_INERTIA; }
}
