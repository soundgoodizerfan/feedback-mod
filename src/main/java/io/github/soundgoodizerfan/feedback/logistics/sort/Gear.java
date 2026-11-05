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
