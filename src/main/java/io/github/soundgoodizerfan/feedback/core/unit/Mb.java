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
