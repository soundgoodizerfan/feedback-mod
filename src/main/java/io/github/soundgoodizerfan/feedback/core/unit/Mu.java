// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.unit;

/**
 * Mass, anchored to the kilogram: one {@code mB} is a litre, so water is exactly
 * {@code 1 Mu/mB} and every other material's {@link Density} reads as its real figure.
 *
 * <p>First made live by the engine design ({@code docs/specs/feedback_vehicles_spec.md} section 6), whose
 * mass is its metal times the material's density -- not a constant, which retires SummerCraft's
 * {@code MASS = 900}.
 */
public record Mu(float value) implements Unit {

    @Override
    public float raw() {
        return value;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.mu";
    }
}
