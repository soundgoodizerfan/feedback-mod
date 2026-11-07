// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.unit;

/**
 * Pressure, in Pu. First live use: {@code PressureVesselBlockEntity}.
 *
 * <h2>An amount over a volume, the same shape as {@code ThermalMass} turns Work into Tu</h2>
 * A vessel does not store Pu directly -- it stores a quantity of air (an {@code int}, the same
 * "how much air" figure {@code Blown} already deals in) and has a fixed volume, and pressure is
 * the quotient, computed on read rather than stored as its own field. Read PneumaticCraft:
 * Repressurized's {@code IAirHandler} before touching this (GPL-3.0, this project's own licence;
 * see {@code THIRD-PARTY-LICENSES.md}) -- {@code pressure = air / volume} is taken directly from
 * there, and it is also exactly PneumaticCraft's own justification for why level is not a pure
 * function of quantity in {@code feedback_mechanics.md} §1.2's XP speculation: a bigger reservoir
 * reads lower for the same stock. Pressure is the second place that shape turns out to be right.
 */
public record Pu(float value) implements Unit {

    @Override
    public float raw() {
        return value;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.pu";
    }
}
