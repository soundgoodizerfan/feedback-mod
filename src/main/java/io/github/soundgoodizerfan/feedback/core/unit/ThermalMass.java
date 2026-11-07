// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.unit;

/**
 * How much Work it takes to raise a body by one Tu: {@code Work/Tu}.
 *
 * <h2>This is the number an upgrade changes</h2>
 * A larger vessel is not faster and it is not more efficient. It is <em>heavier</em>, and
 * everything the player notices about it follows from that one figure: mass over conductance is
 * the time constant, so a heavy vessel is slow to heat, slow to cool, slow to overshoot and slow
 * to respond. Neither end of that is better. A process that needs to hold a narrow window wants
 * the large crucible; a process that needs to move wants the small one.
 *
 * <p>Not to be confused with {@link Conductance}, which is {@code Work/t/Tu} -- a rate of flow
 * rather than a capacity, and the reason the two accessors here are named after their dimensions
 * rather than both being {@code value()}.
 */
public record ThermalMass(float workPerTu) implements Unit {

    @Override
    public float raw() {
        return workPerTu;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.thermal_mass";
    }
}
