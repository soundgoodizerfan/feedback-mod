// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.hydride;

import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;

/** Figures for the hydride bed. Rates are placeholders. */
public final class HydrideTuning {

    private HydrideTuning() {
    }

    /** Ingots a bed holds. */
    public static final int MAX_INGOTS = 9;
    /** Volume of one ingot, mB (the ingot's own, so uptake is real volumes of gas per volume of metal). */
    public static final int INGOT_MB = 144;
    /** Placeholder. Most hydrogen a bed takes from a pipe in a tick, cold. */
    public static final int ABSORB_MB_PER_TICK = 50;
    /** Placeholder. Hydrogen a hot bed gives back per tick for each 100 Tu it stands above its release temperature. */
    public static final float RELEASE_MB_PER_TICK_PER_100_TU = 25f;
    /** Placeholder. How far past its release temperature a loaded bed can be taken before the metal cracks as the gas leaves too fast (Tu). */
    public static final float SAFE_OVER_TU = 100f;
    /** Placeholder. Condition lost per tick for each 100 Tu past the safe margin. */
    public static final float WEAR_PER_TICK_PER_100_TU = 0.0005f;
    /** The worn bed still holds half as much; it never fails outright. */
    public static final float CONDITION_FLOOR = 0.5f;
    public static final ThermalMass MASS = new ThermalMass(8f);
    public static final Conductance LEAK = new Conductance(0.04f);
}
