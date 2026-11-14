// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.unit;

/**
 * Work it takes to change the phase of one {@code Mu} of a material at its melting or boiling point, with
 * no change in temperature: {@code Work/Mu}. At 1000 J per Work this is the real latent heat in kJ/kg
 * (water boils at 2257, ice melts at 334).
 */
public record LatentHeat(float workPerMu) implements Unit {

    @Override
    public float raw() {
        return workPerMu;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.latent_heat";
    }
}
