// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.unit;

/**
 * Radiation, in Ru: a dose rate -- how much a body takes each second. A radioactive material's file
 * gives its figure for one ingot's volume (144 mB) standing at point blank; a source farther away
 * gives less, a shield between gives less again ({@code core/hazard/Radiation}). The dose a body
 * has taken is Ru times seconds, held as a bare float on the player: a total, not a rate.
 */
public record Ru(float value) implements Unit {

    @Override
    public float raw() {
        return value;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.ru";
    }
}
