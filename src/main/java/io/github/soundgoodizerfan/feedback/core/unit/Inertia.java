// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.unit;

/**
 * Resistance to a change in speed: {@code Su x t / RPM}.
 *
 * <h2>Where the dimension comes from</h2>
 * Acceleration is net torque over moment of inertia -- {@code (capacity - load) / inertia} RPM per
 * tick -- which is the real relationship rather than a fudge. Rearranged, inertia is Su divided by
 * RPM-per-tick, so its unit carries a tick in it. Writing that down is worth doing because the
 * figures have to be calibrated against the {@link Su} figures or momentum is simply invisible: at
 * the first values tried, a water wheel reached full speed in one tick.
 *
 * <h2>What it buys, and why Create does not have it</h2>
 * Create's networks change speed instantly. Ours do not, and the difference is not decoration --
 * it is what makes a flywheel a component you can point at, a clutch something that coasts rather
 * than stops, and a heavy network visibly reluctant. "Clutches coast" needed no code; it fell out
 * of this number existing.
 *
 * <h2>See {@link Drag}</h2>
 * for why these two are separate types despite being neighbours in {@code FTuning} and describing
 * superficially similar things.
 */
public record Inertia(float suTicksPerRpm) implements Unit {

    @Override
    public float raw() {
        return suTicksPerRpm;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.inertia";
    }
}
