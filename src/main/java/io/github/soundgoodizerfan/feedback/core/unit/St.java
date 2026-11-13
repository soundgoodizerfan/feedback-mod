// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.unit;

/**
 * The strength of a single application of force -- one blow, one press, one stroke.
 *
 * <h2>Per-application, never a rate</h2>
 * The distinction from {@link Fu} is the one §17 spends the most words on. {@code St} is how hard
 * one blow lands; {@code Fu} is how much has landed in total. A hammer that strikes twice as
 * often delivers twice the Fu at the same St, and a material with a hardness floor above that St
 * takes <em>nothing</em> however long it is hit -- which is philosophy 8's hard gate, and the
 * reason the two are separate units rather than one energy figure.
 *
 * <h2>Where the force goes when it cannot go into the work</h2>
 * A blow geared past what a machine's construction can take is not discarded. The machine absorbs
 * the excess and wears, so St also indexes damage: a machine head's St ceiling is a property of
 * the head, and everything above it is condition lost. Wear is caused only by misuse, so a
 * worn machine is evidence of a specific mistake rather than a maintenance bill -- and condition
 * itself is deliberately <em>not</em> a unit, because it is a dimensionless fraction of as-built
 * spec. See {@code CLAUDE.md}: "wear is a percentage, not a unit".
 */
public record St(float value) implements Unit {

    @Override
    public float raw() {
        return value;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.st";
    }
}
