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
package io.github.soundgoodizerfan.feedback.control;

/**
 * A part a controller can set to a level, not just switch: a throttle, a steering servo, a brake.
 * Philosophy 13's one exception -- {@code docs/specs/feedback_vehicles_spec.md} section 5, <em>Controls</em>:
 * anything in a vehicle may be analog, and nothing else is. Only vehicle parts implement this, so
 * a number reaching a factory's clutch moves nothing; the boundary lives in which blocks exist,
 * not in a check.
 */
public interface Analog {

    /** Sets input {@code channel} (see {@link #channelKeys}) to {@code level}; each part says what a level means. */
    void setLevel(int channel, float level);

    /** Lang keys naming the inputs, by channel: the Programmer shows these on an actuator card. */
    String[] channelKeys();
}
