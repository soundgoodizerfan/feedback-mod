// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.vessel;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;

/**
 * The whole of what tells the six self-fired vessels apart -- Furnace, Smoker, Crude Blast
 * Furnace, Improved Furnace, Kiln and Annealing Furnace -- one class, one
 * {@link io.github.soundgoodizerfan.feedback.machine.vessel.ThermalVesselBlockEntity}, four
 * numbers per vessel (§15). No vessel needs to know it is the food appliance or the ore
 * appliance; the numbers alone decide what each can hold and reach.
 */
public enum VesselKind {

    /** No thermowell -- see {@link #IMPROVED_FURNACE} for why that moved rather than doubled up. */
    FURNACE(FTuning.FURNACE_MASS, FTuning.FURNACE_LEAK, Float.MAX_VALUE, false, 0f),
    SMOKER(FTuning.SMOKER_MASS, FTuning.SMOKER_LEAK, FTuning.SMOKER_CEILING_TU.value(), false, FTuning.SMOKER_SMOKE),
    BLAST_FURNACE(FTuning.BLAST_FURNACE_MASS, FTuning.BLAST_FURNACE_LEAK, Float.MAX_VALUE, false, 0f),
    /** See {@link FTuning#IMPROVED_FURNACE_MASS}. */
    IMPROVED_FURNACE(FTuning.IMPROVED_FURNACE_MASS, FTuning.IMPROVED_FURNACE_LEAK, Float.MAX_VALUE, true, 0f),
    /** See {@link FTuning#KILN_CEILING_TU} for why its ceiling, not its mass or leak, is the
     * whole of what keeps it a ceramics appliance rather than a cheap Blast Furnace. */
    KILN(FTuning.KILN_MASS, FTuning.KILN_LEAK, FTuning.KILN_CEILING_TU.value(), false, 0f, true),
    /** See {@link FTuning#ANNEALING_FURNACE_CEILING_TU}. */
    ANNEALING_FURNACE(FTuning.ANNEALING_FURNACE_MASS, FTuning.ANNEALING_FURNACE_LEAK,
            FTuning.ANNEALING_FURNACE_CEILING_TU.value(), false, 0f, true),
    /** A reverberatory hearth with a rabble turned by a shaft: a row that names a rabble band runs here and nowhere else. */
    PUDDLING_FURNACE(FTuning.PUDDLING_FURNACE_MASS, FTuning.PUDDLING_FURNACE_LEAK, Float.MAX_VALUE, false, 0f, false, true);

    private final ThermalMass mass;
    private final Conductance leak;
    private final float ceilingTu;
    private final boolean thermowell;
    private final float smoke;
    private final boolean sealed;
    private final boolean rotates;

    VesselKind(ThermalMass mass, Conductance leak, float ceilingTu, boolean thermowell, float smoke) {
        this(mass, leak, ceilingTu, thermowell, smoke, false);
    }

    /** {@code sealed}: closed chamber, no air reaches the load unless a bellows is blowing -- see {@link #sealed()}. */
    VesselKind(ThermalMass mass, Conductance leak, float ceilingTu, boolean thermowell, float smoke, boolean sealed) {
        this(mass, leak, ceilingTu, thermowell, smoke, sealed, false);
    }

    /** {@code rotates}: a shaft enters the back and turns a rabble through the charge -- see {@link #rotates()}. */
    VesselKind(ThermalMass mass, Conductance leak, float ceilingTu, boolean thermowell, float smoke, boolean sealed, boolean rotates) {
        this.rotates = rotates;
        this.sealed = sealed;
        this.mass = mass;
        this.leak = leak;
        this.ceilingTu = ceilingTu;
        this.thermowell = thermowell;
        this.smoke = smoke;
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

    /** How much smoke it lays on food cooking in it: 0 for a vessel that vents it (food spec 8). */
    public float smoke() {
        return smoke;
    }

    /** Whether this vessel can be watched passively -- see {@code ThermalBody.hasThermowell}. */
    /** Whether the chamber is closed to air (Kiln, Annealing Furnace); gates {@code airless} thermal processes. */
    public boolean sealed() {
        return sealed;
    }

    /** Whether a shaft turns something inside it (the Puddling Furnace's rabble): the block takes a shaft at its back and draws Su from it. */
    public boolean rotates() {
        return rotates;
    }

    public boolean hasThermowell() {
        return thermowell;
    }
}
