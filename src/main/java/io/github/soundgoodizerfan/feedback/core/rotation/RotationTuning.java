// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.rotation;

import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.Su;

/**
 * Tuning constants for the core/rotation code.
 * Every figure is a placeholder or invented unless its comment says it comes from the slice design.
 */
public final class RotationTuning {

    private RotationTuning() {
    }
    /**
     * Invented. How much a shaft resists a change in speed.
     *
     * <h3>How the inertia numbers were picked</h3>
     * Acceleration is net torque over moment of inertia -- {@code (capacity - load) / inertia}
     * RPM per tick -- which is the real relationship and not a fudge. That means the inertia
     * figures have to be calibrated against the Su figures, or momentum is invisible: at the
     * first values tried, a water wheel reached full speed in a single tick.
     * <p>
     * They are set so that a water wheel takes roughly two seconds to come up to speed and a
     * hand crank a little under one. The spread between them is large, and correctly so: a water
     * wheel is an enormous slab of timber and a crank handle is not.
     */
    public static final Inertia SHAFT_INERTIA = new Inertia(2f);

    /**
     * Floor on total network inertia, so a network of one weightless block still takes a moment
     * to change speed instead of snapping. Prevents a divide-by-zero as well.
     */
    public static final Inertia MINIMUM_INERTIA = new Inertia(1f);

    /**
     * Global multiplier on how quickly anything changes speed. 1 means the physics above is used
     * as written.
     * <p>
     * Raise it and machines feel light and twitchy; lower it and everything runs as though in
     * oil. This is the first number to reach for when the mod feels wrong but nothing specific
     * is wrong with it.
     */
    public static final float INERTIA_RESPONSE = 1f;

    /**
     * Floor on braking force, so an utterly frictionless network still eventually stops rather
     * than coasting for the rest of the save.
     */
    public static final Su MINIMUM_BRAKING_SU = new Su(0.5f);

    // Work per blow is deliberately NOT a figure here. It is force divided by the material's
    // hardness -- see Deformation. A machine states the force it can deliver; what that
    // accomplishes is the material's business.
    /**
     * Below this, a coasting network is called stopped. Without a floor, speed approaches zero
     * asymptotically and machines tick forever at 0.0001 RPM.
     */
    public static final Rpm STOPPED_RPM_THRESHOLD = new Rpm(0.05f);

    /**
     * Fraction of capacity above which a network reads as "straining".
     *
     * <h3>Why a warning band exists at all</h3>
     * Acceleration is surplus torque over inertia, so a network at 98% of capacity is not
     * slightly worse than one at 80% -- it takes eighty seconds to reach speed instead of two.
     * That is a real and useful behaviour, and it was completely invisible: the factory simply
     * felt broken. An adjective is the right granularity to fix that with, because it says
     * <em>where to look</em> without handing over the Su figures that calipers are for.
     */
    public static final float STRAINING_LOAD_FRACTION = 0.9f;
}
