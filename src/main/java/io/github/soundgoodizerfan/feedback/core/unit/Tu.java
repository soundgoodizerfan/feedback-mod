// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.unit;

/**
 * A temperature, in Tu.
 *
 * <h2>Tu is a state, not an amount</h2>
 * The units table is strict about this and it is worth restating on the type itself: a body is
 * <em>at</em> a temperature. It does not contain some Tu that could be poured into another body.
 * What moves between bodies is Work, and how much Work it takes to move one Tu is
 * {@link ThermalMass}. That distinction is the reason there is no {@code getHeat()} anywhere in
 * the mod and never will be.
 *
 * <p>Also used for a <em>difference</em> of two temperatures -- an instrument's resolution, a
 * band's width. See the package javadoc for why that looseness was accepted rather than given a
 * fifth type.
 */
public record Tu(float value) implements Unit {

    @Override
    public float raw() {
        return value;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.tu";
    }
}
