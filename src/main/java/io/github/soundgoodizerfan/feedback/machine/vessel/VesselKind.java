// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.vessel;

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
    FURNACE(VesselTuning.FURNACE_MASS, VesselTuning.FURNACE_LEAK, Float.MAX_VALUE, false, 0f),
    SMOKER(VesselTuning.SMOKER_MASS, VesselTuning.SMOKER_LEAK, VesselTuning.SMOKER_CEILING_TU.value(), false, VesselTuning.SMOKER_SMOKE),
    BLAST_FURNACE(VesselTuning.BLAST_FURNACE_MASS, VesselTuning.BLAST_FURNACE_LEAK, Float.MAX_VALUE, false, 0f),
    /** See {@link VesselTuning#IMPROVED_FURNACE_MASS}. */
    IMPROVED_FURNACE(VesselTuning.IMPROVED_FURNACE_MASS, VesselTuning.IMPROVED_FURNACE_LEAK, Float.MAX_VALUE, true, 0f),
    /** See {@link VesselTuning#KILN_CEILING_TU} for why its ceiling, not its mass or leak, is the
     * whole of what keeps it a ceramics appliance rather than a cheap Blast Furnace. */
    KILN(VesselTuning.KILN_MASS, VesselTuning.KILN_LEAK, VesselTuning.KILN_CEILING_TU.value(), false, 0f, true),
    /** See {@link VesselTuning#ANNEALING_FURNACE_CEILING_TU}. */
    ANNEALING_FURNACE(VesselTuning.ANNEALING_FURNACE_MASS, VesselTuning.ANNEALING_FURNACE_LEAK,
            VesselTuning.ANNEALING_FURNACE_CEILING_TU.value(), false, 0f, true),
    /** A reverberatory hearth with a rabble turned by a shaft: a row that names a rabble band runs here and nowhere else. */
    PUDDLING_FURNACE(VesselTuning.PUDDLING_FURNACE_MASS, VesselTuning.PUDDLING_FURNACE_LEAK, Float.MAX_VALUE, false, 0f, false, true);

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

    /**
     * The walls' mass in Mu and the material they are built of: the vessel's own thermal mass is that many Mu times the
     * material's specific heat (the charge's is added by the vessel). The old invented figure (the field) stands only where the
     * material table does not have the wall.
     */
    private record Wall(String material, float mu) {
    }

    private static final java.util.Map<VesselKind, Wall> WALLS = java.util.Map.of(
            FURNACE, new Wall("stone", 24f), SMOKER, new Wall("stone", 18f), BLAST_FURNACE, new Wall("brick", 360f),
            IMPROVED_FURNACE, new Wall("brick", 72f), KILN, new Wall("brick", 36f), ANNEALING_FURNACE, new Wall("brick", 180f),
            PUDDLING_FURNACE, new Wall("brick", 140f));

    public ThermalMass mass() {
        Wall wall = WALLS.get(this);
        if (wall == null)
            return mass;
        var material = io.github.soundgoodizerfan.feedback.process.MaterialTable.get().materials()
                .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("feedback", wall.material()));
        return material == null ? mass : material.thermal().specificHeat()
                .map(h -> new ThermalMass(h.workPerMuPerTu() * wall.mu())).orElse(mass);
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
