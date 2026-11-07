// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.unit;

/**
 * Aerodynamic drag of a vehicle body in one direction of travel: a drag area, in block²
 * (a block is a metre, so this is m²).
 *
 * <h2>Not {@link Drag}, and the name says so</h2>
 * {@link Drag} is bearing friction, {@code Su/RPM} -- a mechanical load on a shaft. This is how
 * hard air pushes back on a moving body. Both are "drag" in English and nothing else in common,
 * so the vehicle one carries a V ({@code docs/specs/feedback_vehicles_spec.md} section 4).
 *
 * <p>Computed once, when the chassis is welded, from the angle-weighted projected area of its
 * plates; the driving model reads it as a number and never re-simulates the shape.
 */
public record Vdrag(float areaBlocks2) implements Unit {

    @Override
    public float raw() {
        return areaBlocks2;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.vdrag";
    }
}
