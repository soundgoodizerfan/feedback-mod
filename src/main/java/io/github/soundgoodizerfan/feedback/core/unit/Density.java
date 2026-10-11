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
 * Density, in {@code Mu/mB} -- kilograms per litre, so water is 1 and steel about 7.85.
 *
 * <h2>A material's figure, not a block's</h2>
 * It lives on {@code process/Material}, read from {@code material/*.json}, because everything
 * made of a material should weigh what that material weighs whatever shape it was given -- an
 * engine, a plate, a tube. Sable's per-block mass is the figure for a placed block; this is the
 * figure for the stuff.
 */
public record Density(float muPerMb) implements Unit {

    @Override
    public float raw() {
        return muPerMb;
    }

    @Override
    public String unitKey() {
        return "feedback.unit.density";
    }
}
