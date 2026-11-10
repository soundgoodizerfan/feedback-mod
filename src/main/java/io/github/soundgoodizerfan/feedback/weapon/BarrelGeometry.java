// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.weapon;

import io.github.soundgoodizerfan.feedback.core.FTuning;

/**
 * A barrel's real shape as the bake reads it: what the drill, the lathe and the cutoff wheel made of
 * it. It is a view, not a stored thing: the three measures are the barrel part's {@code axes}
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
