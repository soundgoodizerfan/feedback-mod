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
package io.github.soundgoodizerfan.feedback.core.electric;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;

/**
 * The electrical rulings that are still open, each in one method so changing one is a one-line
 * edit and not a search (mechanics 6.2, 6.5).
 *
 * <p>Everything here is a decision the user has not closed, or a placeholder behind one. The
 * resolver ({@link ElectricRun}) and the sinks call these and hold no opinion of their own, which
 * is the whole reason they are not inlined where they are used.
 */
public final class ElectricRules {

    private ElectricRules() {
    }

    /** Reference volts of a band. Placeholder anchors, [OPEN] (6.3). */
    public static float bandVolts(VoltageBand band) {
        return switch (band) {
            case LV -> FTuning.ELECTRIC_LV_VOLTS;
            case MV -> FTuning.ELECTRIC_MV_VOLTS;
            case HV -> FTuning.ELECTRIC_HV_VOLTS;
            case UHV -> FTuning.ELECTRIC_UHV_VOLTS;
        };
    }

    /**
     * The band a potential belongs to: the lowest whose reference reaches it, UHV above all. A
     * cell at 1.1 V is LV; so is anything below LV's own figure.
     */
    public static VoltageBand bandOf(float volts) {
        for (VoltageBand band : VoltageBand.values())
            if (volts <= bandVolts(band) + 1e-4f)
                return band;
        return VoltageBand.UHV;
    }

    /**
     * Fraction of its voltage a source holds when drawn at {@code loadRatio} times its rated
     * current. Full up to the rating, then a straight line down to
     * {@link FTuning#SOURCE_SAG_FLOOR} at {@link FTuning#SOURCE_OVERDRAW_LIMIT}, never below.
     * <b>[OPEN]</b> the curve (6.2): a source is never destroyed and never goes to zero volts.
     */
    public static float sagFactor(float loadRatio) {
        if (loadRatio <= 1f)
            return 1f;
        float over = Math.min(loadRatio, FTuning.SOURCE_OVERDRAW_LIMIT) - 1f;
        return 1f - (1f - FTuning.SOURCE_SAG_FLOOR) * over / (FTuning.SOURCE_OVERDRAW_LIMIT - 1f);
    }

    /**
     * How fast a sink runs, 0 to 1: the product of the fraction of its rated voltage it sees and
     * the fraction of its wanted current it gets, each capped at 1. Zero floor, never a refusal
     * (6.5). <b>[OPEN]</b> whether the slow-down is in voltage, in current or in their product;
     * the product is what makes both visible failures (a long run, a weak source) slow the
     * machine, and it is chosen here only so that one method holds the choice.
     */
    public static float rate(float voltsSeen, float ampsDelivered, float ampsWanted, VoltageBand rated) {
        float volts = Math.min(1f, Math.max(0f, voltsSeen) / bandVolts(rated));
        float amps = ampsWanted <= 0f ? 1f : Math.min(1f, ampsDelivered / ampsWanted);
        return volts * amps;
    }

    /**
     * Fraction by which the voltage a sink sees is over its rating, 0 when it is not. This is the
     * force that cannot go into the work (6.5): the caller turns it into condition loss and heat
     * in its own body. <b>[OPEN]</b> the per-unit scale.
     */
    public static float overVoltage(float voltsSeen, VoltageBand rated) {
        return Math.max(0f, voltsSeen / bandVolts(rated) - 1f);
    }
}
