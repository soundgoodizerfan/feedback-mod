// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.unit;

/**
 * Volume, in millibuckets -- exactly a litre (philosophy 17).
 *
 * <p>First made live by air, not by fluids. The bellows, firebox, self-fired vessels and the
 * Pressure Vessel all trade in a volume of air, and before this they did it in bare floats, with
 * the bellows handing its figure over through {@link St} one-to-one. Typing the units is what
 * exposed that; this is the fix the bellows' own {@code [OPEN]} asked for. Air is volume because a
 * gas system is coming and volume is how the mod already measures anything gas-shaped.
 *
 * <p>Fluid tanks still count {@code mB} as NeoForge's bare {@code int}. Retyping them is a separate
 * pass, and not one anything is waiting on.
 */
public record Mb(float value) implements Unit {

    @Override
    public float raw() {
        return value;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.mb";
    }
}
