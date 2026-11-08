// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control;

/**
 * A switch that can also be walked a step at a time: channel 0 is the plain on/off of {@link Switchable}, every other channel a
 * pulse that moves the part one position on the rising edge of a boolean and does nothing while the boolean holds. Still a switch,
 * not a dial: a control rod is moved by a program that decides when to pulse, never told a level.
 */
public interface Steppable extends Switchable {

    /** Lang keys naming the channels, by channel: the Programmer shows these on an actuator card. Channel 0 is the plain switch. */
    String[] channelKeys();

    /** Sets pulse channel {@code channel} (1 or higher) to {@code on}; a step happens on the change to true. */
    void pulse(int channel, boolean on);
}
