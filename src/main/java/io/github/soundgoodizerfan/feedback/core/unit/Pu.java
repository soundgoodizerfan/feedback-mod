/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
