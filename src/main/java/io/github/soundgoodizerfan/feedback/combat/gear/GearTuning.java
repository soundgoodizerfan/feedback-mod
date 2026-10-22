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
package io.github.soundgoodizerfan.feedback.combat.gear;

/**
 * Every figure of combat gear (implementation plan 2.8). Philosophy 14: difficulty stays flat, so gear
 * narrows the ratio of window to enemy speed and never makes a parry free -- hence a cap on each sum
 * ({@code MAX_*}), whatever is worn and however good the material. All placeholder until played.
 *
 * <p>A material's quality is a saturating curve {@code x / (x + half)}: copper (hardness 1) is a tenth
 * of the way, iron (6) four-tenths, steel (15) two-thirds, diamond (100) nearly all of it. A
 * better material always helps and never runs off the scale.
 */
public final class GearTuning {

    /** Half-point of the curves: the hardness, toughness and density at which a material is half-way. */
    public static final float HARDNESS_HALF = 8f, TOUGHNESS_HALF = 30f, DENSITY_HALF = 8f;

    /** Bracer: ticks of parry window a perfect shell adds, and the share of a blow it takes off the weapon's joint. */
    public static final float BRACER_WINDOW = 2f, BRACER_JOINT = 0.5f;
    /** Sash: share of the roll's speed a perfectly light buckle adds, ticks a perfect strap shaves off the recovery, ticks of i-frame a perfect buckle adds. */
    public static final float SASH_DISTANCE = 0.2f, SASH_RECOVERY = 4f, SASH_I_FRAMES = 1f;
    /** Back piece: Mu of carried mass per unit of plate density, per plate. */
    public static final float BACK_MU_PER_DENSITY = 1f;

    /** The most worn gear may add, whatever the slots hold. */
    public static final float MAX_WINDOW = 2f, MAX_JOINT = 0.5f, MAX_ROLL_DISTANCE = 0.2f,
            MAX_RECOVERY_SHAVE = 4f, MAX_I_FRAMES = 1f, MAX_MASS = 40f;

    private GearTuning() {
    }

    /** {@code x / (x + half)}: 0 at nothing, one half at {@code half}, approaching 1. */
    public static float saturate(float x, float half) {
        return x <= 0 ? 0f : x / (x + half);
    }
}
