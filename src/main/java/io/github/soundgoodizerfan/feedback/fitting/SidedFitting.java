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
package io.github.soundgoodizerfan.feedback.fitting;

/**
 * A fitting that occupies one face of its holder -- a {@link SensorFitting}, an
 * {@link AdapterFitting} or a {@link RedstoneAnalogFitting}, and only ever one of those per face, the same one-per-side rule
 * GTCEu's covers use.
 * <p>
 * Sealed rather than left open: {@link Fittable} stores exactly one of these per
 * {@code Direction}, and a third implementor showing up later would be a fourth fitting kind,
 * not a variation on this one -- see {@link Fitting} for why that gets its own type instead.
 */
public sealed interface SidedFitting extends Fitting permits SensorFitting, AdapterFitting, RedstoneAnalogFitting {
}
