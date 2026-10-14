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
package io.github.soundgoodizerfan.feedback.machine.coil;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.electric.ElectricNode;
import io.github.soundgoodizerfan.feedback.core.thermal.AmbientTable;
import io.github.soundgoodizerfan.feedback.core.thermal.Heat;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatSource;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalBody;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;
import io.github.soundgoodizerfan.feedback.machine.Wearing;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A resistive element: the Work the current does in it is its heat, and it is the fire for
 * whatever stands on it (mechanics 6.16).
 *
 * <h2>Its own body, not a flame temperature</h2>
 * A firebox's fire has no mass (see {@link HeatSource}); an element does, and a small one. So the
 * element is a {@link ThermalBody}: each tick the current puts {@code volts x amps x}
 * {@link FTuning#COIL_WORK_PER_WATT_TICK} into it (Joule's law in this mod's units, the one
 * physical constant, as {@code QU_PER_AMP_TICK} is Faraday's), and it gives heat to the body above
 * by the same {@code FIRE_CONDUCTANCE x difference} the body takes from it in {@code Heat.tick} --
 * so what the vessel gains the element loses, and the element settles a little above its load. A
 * fire's temperature is what it burns at; an element's is what its power and its load leave it at.
 * <p>
 * That small mass is electrical heat's character (philosophy 10): cut the supply and the element
 * has almost nothing to coast on. The thermal mass that stays is the vessel's.
 *
 * <h2>The element's limit is its metal's</h2>
 * Above its element's {@code service_temperature} the element softens and the coil wears, by the
 * excess -- force that cannot go into the work goes into the machine. Over-voltage needs no rule of
 * its own: more volts is more heat, and that is where it shows. A copper element is cheap and
 * wears above 300 Tu, an iron one holds to 500: a better element is a hotter coil. Condition
 * multiplies the heat; a coil never breaks.
 */
public class CoilBlockEntity extends ElectricNode implements ThermalBody, HeatSource, Wearing {

    private float temperature = FTuning.AMBIENT_TU.value();
    private float condition = 1f;
    private boolean powered;

    public CoilBlockEntity(BlockPos pos, BlockState state) {
        super(FBlockEntities.COIL.get(), pos, state);
    }

    // --- sink ---------------------------------------------------------------------------------

    @Override
    public Role role() {
        return Role.SINK;
    }

    @Override
    public float wantedAmps() {
        return FTuning.COIL_AMPS;
    }

    @Override
    public VoltageBand ratedBand() {
        return VoltageBand.LV;
    }

    @Override
    public void onResolved(float volts, float amps, float wanted, boolean blocked) {
        super.onResolved(volts, amps, wanted, blocked);
        powered = volts > 0f && amps > 0f;
        if (powered)
            Heat.warm(this, volts * amps * FTuning.COIL_WORK_PER_WATT_TICK * condition);
    }

    /** The hottest the element runs without wearing: its metal's service temperature. */
    public Tu serviceTemperature() {
        Material material = MaterialTable.get().materials().get(((CoilBlock) getBlockState().getBlock()).element);
        return material == null ? FTuning.AMBIENT_TU
                : material.thermal().serviceTemperature().orElse(FTuning.AMBIENT_TU);
    }

    public void tickServer() {
        if (level == null)
            return;
        Tu ambient = AmbientTable.at(level);
        // What the body above takes in its own Heat.tick, under the same condition it applies.
        if (temperature > ambient.value()
                && level.getBlockEntity(worldPosition.above()) instanceof ThermalBody load)
            Heat.warm(this, -FTuning.FIRE_CONDUCTANCE.workPerTickPerTu()
                    * (temperature - load.getTemperature().value()));
        Heat.tick(this, ambient, ambient, false);

        float excess = temperature - serviceTemperature().value();
        if (excess > 0f) {
            condition = Math.max(FTuning.COIL_CONDITION_FLOOR, condition - FTuning.COIL_WEAR_PER_TU_EXCESS * excess);
            setChanged();
        }
    }

    // --- HeatSource ---------------------------------------------------------------------------

    @Override
    public Tu getFireTu() {
        return new Tu(temperature);
    }

    // --- Wearing ------------------------------------------------------------------------------

    @Override
    public float getCondition() {
        return condition;
    }

    @Override
    public boolean isRunning() {
        return powered;
    }

    // --- ThermalBody --------------------------------------------------------------------------

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
        return FTuning.COIL_MASS;
    }

    @Override
    public Conductance getLeak() {
        return FTuning.COIL_LEAK;
    }

    // --- persistence --------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putFloat("Temperature", temperature);
        tag.putFloat("Condition", condition);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Temperature"))
            temperature = tag.getFloat("Temperature");
        condition = tag.contains("Condition") ? tag.getFloat("Condition") : 1f;
    }
}
