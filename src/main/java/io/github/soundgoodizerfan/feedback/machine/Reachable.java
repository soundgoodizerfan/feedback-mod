// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine;

/**
 * A machine that states what it can reach: the hottest it runs, the pressure it holds to its danger line and the speed it turns to
 * its rim. An equipment spec, printed exactly by the reach probe; a figure the machine has no limit in is left at "unstated".
 */
public interface Reachable {

    Reach reach();

    /** Hottest temperature, Tu; pressure danger line, Pu; fastest turning, RPM. {@link #UNSTATED} where the machine states none. */
    record Reach(float tempTu, float pressurePu, float rpm) {

        public static final float UNSTATED = Float.MAX_VALUE;

        public static Reach temperature(float tu) {
            return new Reach(tu, UNSTATED, UNSTATED);
        }

        public static Reach pressure(float pu) {
            return new Reach(UNSTATED, pu, UNSTATED);
        }

        public static Reach speed(float rpm) {
            return new Reach(UNSTATED, UNSTATED, rpm);
        }
    }
}
