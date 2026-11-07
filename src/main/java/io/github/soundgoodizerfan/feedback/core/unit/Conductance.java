// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.unit;

/**
 * How readily Work crosses a boundary, in {@code Work/t} per Tu of difference across it:
 * {@code Work/t/Tu}.
 *
 * <h2>One type, two roles, and noticing that was the point of the exercise</h2>
 * A fire's grip on the vessel above it and a vessel's leak to the room are the same kind of
 * quantity, and the code did not say so. {@code Heat.equilibrium} adds them --
 * {@code (conductance x fire + leak x ambient) / (conductance + leak)} -- which is only a legal
 * thing to write because they share a dimension, and until this type existed nothing recorded
 * that. A leak <em>is</em> a conductance; the boundary it crosses is the one between the vessel
 * and the room, and calling it a leak is a statement about whether you wanted it, not about what
 * it is.
 *
 * <h2>Why heat must flow on a difference</h2>
 * Restated here because this is the type that carries it. The first version had a fire deliver a
 * flat {@code Work/t} with no conductance in it, and a vessel then settles where supply equals
 * leak -- so halving the leak doubles the final temperature and insulation runs away without
 * limit. Insulation became a trap instead of an upgrade.
 * <p>
 * Multiplying by {@code (fire - vessel)} fixes it by construction: a vessel approaches its fire's
 * temperature and can never pass it, insulation means getting closer to the flame and holding
 * there, and the only way past a flame is a hotter flame. See
 * {@link io.github.soundgoodizerfan.feedback.core.thermal.Heat} for the full argument.
 */
public record Conductance(float workPerTickPerTu) implements Unit {

    @Override
    public float raw() {
        return workPerTickPerTu;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.conductance";
    }
}
