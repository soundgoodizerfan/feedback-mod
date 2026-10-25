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
package io.github.soundgoodizerfan.feedback.combat.roll;

/** The wall kick's figures. Placeholders to be felt in play. Ticks are server ticks; speeds are blocks per tick. */
public final class WallKickTuning {

    /** How long after sprinting or rolling on the ground the momentum still counts (1.5 s: a sprint-jump into a wall). */
    public static final int MOMENTUM_TICKS = 24;
    /** Speed given away from the wall, and upward (a normal jump is 0.42). */
    public static final double PUSH = 0.5, UP = 0.55;
    /** Stamina one wall kick spends; hunger is paid when it refills ({@code Stamina.EXHAUSTION_PER_POINT}). */
    public static final float STAMINA = 10f;

    private WallKickTuning() {
    }
}
