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
package io.github.soundgoodizerfan.feedback.core.thermal;

/**
 * What a flame did across a gap nobody was there to tick: for how many of the gap's ticks it
 * burned, and at what temperature, on average.
 *
 * <p>One span and not a curve, on purpose. A fuel burns until it is spent and then the fire is
 * out -- lit first, dark after, never the other way round -- so a figure for the lit part and the
 * length of it is the whole shape. The temperature is the time-weighted mean of the pieces that
 * burned, which is exact for one piece and a fair statement of many; the noise floor
 * (philosophy 8) is not worth simulating for a stretch the player was not watching.
 *
 * @param litTicks ticks of the gap the fire was alight, from its start.
 * @param tu       its mean flame temperature over those ticks; meaningless when {@code litTicks} is 0.
 */
public record FireSpan(long litTicks, float tu) {

    /** No fire at all: a cold firebox, or nothing under the vessel. */
    public static final FireSpan NONE = new FireSpan(0L, 0f);

    public FireSpan clampedTo(long gap) {
        return litTicks <= gap ? this : new FireSpan(gap, tu);
    }
}
