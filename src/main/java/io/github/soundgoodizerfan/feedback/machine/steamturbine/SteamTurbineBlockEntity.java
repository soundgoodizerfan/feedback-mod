// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
