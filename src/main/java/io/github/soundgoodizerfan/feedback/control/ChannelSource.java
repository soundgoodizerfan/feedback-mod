// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control;

/**
 * A data node with several readings, picked by channel: a Driver's Seat, read key by key. A
 * Sensor card linked to one carries the channel it reads.
 */
public interface ChannelSource {

    /** Reading on {@code channel}; zero for a channel with nothing on it. */
    float read(int channel);

    /** Whether this node offers channels at all; a part that is only sometimes a source (a belt cell fitted with a counter) says no otherwise. */
    default boolean offersChannels() {
        return true;
    }

    /** The channel a freshly linked card starts on. */
    default int defaultChannel() {
        return 0;
    }
}
