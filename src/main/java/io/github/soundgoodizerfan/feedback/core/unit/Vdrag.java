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
 * Aerodynamic drag of a vehicle body in one direction of travel: a drag area, in block²
 * (a block is a metre, so this is m²).
 *
 * <h2>Not {@link Drag}, and the name says so</h2>
 * {@link Drag} is bearing friction, {@code Su/RPM} -- a mechanical load on a shaft. This is how
 * hard air pushes back on a moving body. Both are "drag" in English and nothing else in common,
 * so the vehicle one carries a V ({@code docs/feedback_vehicles_spec.md} section 4).
 *
 * <p>Computed once, when the chassis is welded, from the angle-weighted projected area of its
 * plates; the driving model reads it as a number and never re-simulates the shape.
 */
public record Vdrag(float areaBlocks2) implements Unit {

    @Override
    public float raw() {
        return areaBlocks2;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.vdrag";
    }
}
