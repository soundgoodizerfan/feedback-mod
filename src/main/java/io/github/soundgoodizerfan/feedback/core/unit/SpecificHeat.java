// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.unit;

/**
 * Work it takes to raise one {@code Mu} of a material by one {@code Tu}: {@code Work/Mu/Tu}. With
 * {@code FTuning.JOULES_PER_WORK} at 1000 this is the real specific heat in kJ/(kg K) unchanged, so water
 * is 4.18 and iron 0.45. A body's {@link ThermalMass} is this times its mass.
 */
public record SpecificHeat(float workPerMuPerTu) implements Unit {

    @Override
    public float raw() {
        return workPerMuPerTu;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.specific_heat";
    }
}
