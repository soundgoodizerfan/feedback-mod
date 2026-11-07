// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.sort;

/**
 * The era of a piece of sorting gear, which decides what it does with cargo it was not set for
 * (design digest: "a machine only knows what it is told"). Earlier gear sits toward the bad end.
 */
public enum Gear {
    /** Crude belt modules: mechanical and passive. */
    CRUDE,
    /** Early tube junction. */
    TUBE_EARLY,
    /** Optical or controller-driven belt gear. */
    OPTICAL,
    /** Late tube junction, programmed. */
    PROGRAMMED,
    /** An ender pair. */
    SPATIAL;

    /** What the gear does with a unit it cannot place. */
    public enum Handling {
        /** Carried on to the end of the line (jams there). */
        PASS_ALONG,
        /** Sent to the first free or default output. */
        DEFAULT_SIDE,
        /** Sent to a reject outlet; a full outlet jams. */
        REJECT,
        /** Returned on a return line; never jams while the line has room. */
        RETURN,
        /** Not moved at all. */
        REFUSE
    }

    public Handling unexpected() {
        return switch (this) {
            case CRUDE -> Handling.PASS_ALONG;
            case TUBE_EARLY -> Handling.DEFAULT_SIDE;
            case OPTICAL -> Handling.REJECT;
            case PROGRAMMED -> Handling.RETURN;
            case SPATIAL -> Handling.REFUSE;
        };
    }
}
