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
package io.github.soundgoodizerfan.feedback.machine.vessel;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;

/**
 * The whole of what tells a Furnace, a Smoker and a Crude Blast Furnace apart -- one class, one
 * {@link io.github.soundgoodizerfan.feedback.machine.vessel.ThermalVesselBlockEntity}, four
 * numbers per vessel (§15). No vessel needs to know it is the food appliance or the ore
 * appliance; the numbers alone decide what each can hold and reach.
 */
public enum VesselKind {

    /** No thermowell -- see {@link #IMPROVED_FURNACE} for why that moved rather than doubled up. */
    FURNACE(FTuning.FURNACE_MASS, FTuning.FURNACE_LEAK, Float.MAX_VALUE, false),
    SMOKER(FTuning.SMOKER_MASS, FTuning.SMOKER_LEAK, FTuning.SMOKER_CEILING_TU.value(), false),
    BLAST_FURNACE(FTuning.BLAST_FURNACE_MASS, FTuning.BLAST_FURNACE_LEAK, Float.MAX_VALUE, false),
    /** See {@link FTuning#IMPROVED_FURNACE_MASS}. */
    IMPROVED_FURNACE(FTuning.IMPROVED_FURNACE_MASS, FTuning.IMPROVED_FURNACE_LEAK, Float.MAX_VALUE, true),
    /** See {@link FTuning#KILN_CEILING_TU} for why its ceiling, not its mass or leak, is the
     * whole of what keeps it a ceramics appliance rather than a cheap Blast Furnace. */
    KILN(FTuning.KILN_MASS, FTuning.KILN_LEAK, FTuning.KILN_CEILING_TU.value(), false),
    /** See {@link FTuning#ANNEALING_FURNACE_CEILING_TU}. */
    ANNEALING_FURNACE(FTuning.ANNEALING_FURNACE_MASS, FTuning.ANNEALING_FURNACE_LEAK,
            FTuning.ANNEALING_FURNACE_CEILING_TU.value(), false);

    private final ThermalMass mass;
    private final Conductance leak;
    private final float ceilingTu;
    private final boolean thermowell;

    VesselKind(ThermalMass mass, Conductance leak, float ceilingTu, boolean thermowell) {
        this.mass = mass;
        this.leak = leak;
        this.ceilingTu = ceilingTu;
        this.thermowell = thermowell;
    }

    public ThermalMass mass() {
        return mass;
    }

    public Conductance leak() {
        return leak;
    }

    public float ceilingTu() {
        return ceilingTu;
    }

    /** Whether this vessel can be watched passively -- see {@code ThermalBody.hasThermowell}. */
    public boolean hasThermowell() {
        return thermowell;
    }
}
