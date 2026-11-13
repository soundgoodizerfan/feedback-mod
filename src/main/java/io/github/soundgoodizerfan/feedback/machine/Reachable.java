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
    record Reach(float tempTu, float pressurePu, float rpm, float flowMbPerRpm, float gearLow, float gearHigh) {

        public static final float UNSTATED = Float.MAX_VALUE;

        public static Reach temperature(float tu) {
            return new Reach(tu, UNSTATED, UNSTATED, UNSTATED, UNSTATED, UNSTATED);
        }

        public static Reach pressure(float pu) {
            return new Reach(UNSTATED, pu, UNSTATED, UNSTATED, UNSTATED, UNSTATED);
        }

        public static Reach speed(float rpm) {
            return new Reach(UNSTATED, UNSTATED, rpm, UNSTATED, UNSTATED, UNSTATED);
        }

        /** A pump: the mB it moves each tick for each RPM it turns, as built and as worn. */
        public static Reach flow(float mbPerRpm) {
            return new Reach(UNSTATED, UNSTATED, UNSTATED, mbPerRpm, UNSTATED, UNSTATED);
        }

        /** A transmission: input turns per output turn in its lowest and its highest gear (torque is multiplied by the same figure, less what the gears lose). */
        public static Reach gears(float low, float high) {
            return new Reach(UNSTATED, UNSTATED, UNSTATED, UNSTATED, low, high);
        }
    }
}
