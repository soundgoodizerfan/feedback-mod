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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.core.FTuning;

/**
 * A barrel's real shape as the bake reads it: what the drill, the lathe and the cutoff wheel made of
 * it. It is a view, not a stored thing: the three measures are the barrel part's {@code dims}
 * ({@code bore}, {@code outer}, {@code length}; {@link io.github.soundgoodizerfan.feedback.modular.ModularType.Dim}),
 * carried on the part beside its material, and written by {@link io.github.soundgoodizerfan.feedback.process.Machining}.
 *
 * @param boreMm  inside diameter, grown by drilling.
 * @param outerMm outside diameter, thinned by turning.
 * @param lengthM length, shortened by the cutoff wheel.
 */
public record BarrelGeometry(float boreMm, float outerMm, float lengthM) {

    /** The names the barrel role declares its measures under. */
    public static final String BORE = "bore", OUTER = "outer", LENGTH = "length";

    /** A barrel as the drill first pierces it. */
    public static final BarrelGeometry STOCK = new BarrelGeometry(FTuning.GUN_BARREL_PILOT_MM,
            FTuning.GUN_BARREL_STOCK_MM, FTuning.GUN_BARREL_STOCK_M);

    /** Wall thickness, what the chamber's rating is read from. */
    public float wallMm() {
        return (outerMm - boreMm) / 2;
    }

    /** Whether there is still a barrel here: a wall left, and a length. Past either it is swarf. */
    public boolean whole() {
        return wallMm() >= FTuning.GUN_BARREL_MIN_WALL_MM && lengthM >= FTuning.GUN_BARREL_MIN_M;
    }
}
