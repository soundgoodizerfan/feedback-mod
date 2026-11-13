// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.fluid;

import io.github.soundgoodizerfan.feedback.core.unit.Pu;

/**
 * Tuning constants for the core/fluid code.
 * Every figure is a placeholder or invented unless its comment says it comes from the slice design.
 */
public final class FluidTuning {

    private FluidTuning() {
    }
    /**
     * Invented. Above this many {@link io.github.soundgoodizerfan.feedback.core.unit.Pu}, a
     * Pressure Vessel starts risking an explosion every tick, at a chance that climbs the further
     * past this line it sits -- read from PneumaticCraft: Repressurized's {@code
     * MachineAirHandler#addAir} (GPL-3.0, this project's own licence). Below it: perfectly safe,
     * however long it sits there. This is the hard gate that gives {@code
     * feedback_philosophy.md} §14's named GregTech-boiler-explosion precedent something to
     * actually happen to, closing the "exploding boiler" open item in {@code TODO.md} §5a for
     * Feedback's own equipment in general rather than for the Boiler specifically.
     */
    public static final Pu PRESSURE_VESSEL_DANGER_PU = new Pu(5f);

    /** Invented. At or above this many Pu, a Pressure Vessel explodes outright, every tick. */
    public static final Pu PRESSURE_VESSEL_CRITICAL_PU = new Pu(7f);


    // --- petrochemistry, phase 1 (spec_petrochemistry): every figure below is invented ----------------

    /** Invented. A pipe run: mB each pipe segment adds to the run's ledger, and the most mB a tick a run hands to what it touches. */
    public static final int PIPE_VOLUME_MB = 500, PIPE_FLOW_MB_PER_TICK = 200;


    // --- pressure on pipes, the compressor and the gas tank (spec 5.2, 8.3): invented ---------------

    /** Invented. A run's ledger holds this many times its liquid volume of gas, but only a compressor can push past one times; Pu a run reads per volume of gas held. */
    public static final int PIPE_COMPRESSION_HEADROOM = 3;

    public static final float PIPE_PU_PER_VOLUME = 2f, PIPE_GAS_FLOW_PER_PU = 100f, PIPE_BURST_POWER = 1.5f;
}
