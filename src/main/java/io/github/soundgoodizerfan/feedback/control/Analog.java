// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
