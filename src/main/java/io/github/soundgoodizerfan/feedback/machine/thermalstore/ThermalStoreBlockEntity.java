// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.thermalstore;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Catchup;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The grid's bulk store (grid spec 2.3): a huge thermal mass that holds heat, not electricity. It is a body like the Hearth
 * ({@link HeatSource#at} under it is the fire that charges it: a coil, an arc head, a heat exchanger or a firebox) and a fire to
 * what stands on it, so a Boiler set on top draws it down into steam for the turbine. It adds no rule of its own: the round trip
 * is whatever the fire's contact, the leak to air and the boiler's own losses leave, which is poor by construction. It cannot get
 * hotter than what charges it, and it cools toward ambient at {@link FTuning#THERMAL_STORE_LEAK}.
 */
public class ThermalStoreBlockEntity extends BlockEntity implements ThermalBody, HeatSource {

    private float temperature = FTuning.AMBIENT_TU.value();
    private final Catchup offline = new Catchup();

    public ThermalStoreBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.THERMAL_STORE.get(), pos, state);
    }

    @Override
    public Tu getTemperature() {
        return new Tu(temperature);
    }

    @Override
    public void setTemperature(Tu tu) {
        temperature = tu.value();
    }

    @Override
    public ThermalMass getThermalMass() {
        return FTuning.THERMAL_STORE_MASS;
    }

    @Override
    public Conductance getLeak() {
        return FTuning.THERMAL_STORE_LEAK;
    }

    @Override
    public Tu getFireTu() {
        return getTemperature();
    }

    public void tickServer() {
        if (level == null)
            return;
        Tu fireTu = HeatSource.at(level, worldPosition);
        Tu ambient = AmbientTable.at(level);
        long away = offline.take(level);
        if (away > 0L)
            Catchup.settle(this, HeatSource.spanAt(level, worldPosition, away), ambient, away);
        Heat.tick(this, fireTu, ambient, fireTu.value() > ambient.value());
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Temperature", temperature);
        offline.save(tag, level);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : FTuning.AMBIENT_TU.value();
        offline.load(tag);
    }
}
