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
package io.github.soundgoodizerfan.feedback.machine.refrigeration;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricRules;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.Refrigerable;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A heat pump (food spec 8, Refrigeration). A sink: while it is fed it holds every
 * {@link Refrigerable} touching it cold with no ice. It has no thermostat and no switch; the
 * player's breaker is the controller.
 *
 * <p>A pump moves heat, it does not delete it: running drives the unit's own temperature up toward
 * a ceiling and off lets it fall, so its radiator burns a bare hand ({@code HotContact} reads any
 * {@link ThermalBody}). That hot spot and the Eu drawn are the whole cost.
 */
public class RefrigerationUnitBlockEntity extends ElectricNode implements ThermalBody {

    private float temperature = FTuning.AMBIENT_TU.value();

    public RefrigerationUnitBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.REFRIGERATION_UNIT.get(), pos, state);
    }

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return FTuning.REFRIGERATION_AMPS;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.LV;
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        if (level == null)
            return;
        boolean running = ElectricRules.rate(volts, amps, wanted, ratedBand()) > 0f;
        Tu ambient = AmbientTable.at(level);
        Heat.tick(this, running ? FTuning.REFRIGERATION_HOT_TU : ambient, ambient, running);
        // Refreshed well inside the box's lapse, so a dead unit stops holding about a second on.
        if (running && level.getGameTime() % 10 == 0)
            for (Direction side : Direction.values())
                if (level.getBlockEntity(worldPosition.relative(side)) instanceof Refrigerable box)
                    box.holdCold();
        setChanged();
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
        return FTuning.REFRIGERATION_MASS;
    }

    @Override
    public Conductance getLeak() {
        return FTuning.REFRIGERATION_LEAK;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Temperature", temperature);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("Temperature") ? tag.getFloat("Temperature") : FTuning.AMBIENT_TU.value();
    }
}
