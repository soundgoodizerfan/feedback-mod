// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.fitting;

import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.machine.vessel.VesselTuning;

/**
 * Tuning constants for the fitting code.
 * Every figure is a placeholder or invented unless its comment says it comes from the slice design.
 */
public final class FittingTuning {

    private FittingTuning() {
    }
    /**
     * Invented. The thermal mass a Refractory Lining adds to the vessel it is fitted in, in
     * Work/Tu -- the first concrete {@code UpgradeFitting}.
     *
     * <h3>Why mass and not leak</h3>
     * The block above settles insulation: leak is something you build around a vessel, and a
     * second route to the same number as an item would be a duplicate answer, not a second one.
     * Mass is the other figure a vessel has, and a lining is literally more of the vessel -- a
     * thicker, denser wall. It is the same figure {@link VesselTuning#CRUCIBLE_SMALL_MASS} and
     * {@link MachineTuning#CRUCIBLE_LARGE_MASS} differ by, so it lands a small crucible partway to a large one.
     *
     * <h3>The trade, which is not a strict win</h3>
     * Mass moves nothing about where a vessel settles ({@code Heat.equilibrium} has no mass in
     * it), so it can never carry a vessel past its flame. It only stretches the time constant,
     * both ways: slower to climb (more fuel burnt getting there), slower to come back down, slower
     * to answer a thermostat. A batch that needs a rate limit held wants it; a job that needs to
     * <em>move</em> wants the bare small crucible. Doubles a small crucible and adds a tenth to a
     * large one, so it is the small crucible's way to buy steadiness without the large block.
     */
    public static final ThermalMass REFRACTORY_LINING_MASS = new ThermalMass(25f);

    /**
     * SLICE: a fitted temperature sensor resolves finer than the carried thermometer.
     * <p>
     * It is bolted to one vessel rather than carried everywhere, so the trade for that loss of
     * flexibility is a number a purpose-built fixture can actually hold. Still not zero -- §8's
     * noise floor applies here exactly as it does to every other instrument.
     */
    public static final Tu TEMPERATURE_SENSOR_RESOLUTION_TU = new Tu(10f);

    public static final float CURRENT_SENSOR_RESOLUTION_AMPS = 0.5f;
}
