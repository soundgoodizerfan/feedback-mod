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
package io.github.soundgoodizerfan.feedback.machine.column;

import io.github.soundgoodizerfan.feedback.core.FTuning;

/** The discrete tray a player chooses (spec 6.4): sharper cuts against flow against how fast it cokes. */
public enum TrayType {
    BUBBLE_CAP(FTuning.TRAY_BUBBLE_CAP_EFFICIENCY, FTuning.TRAY_BUBBLE_CAP_FLOW, FTuning.TRAY_BUBBLE_CAP_FOUL),
    SIEVE(FTuning.TRAY_SIEVE_EFFICIENCY, FTuning.TRAY_SIEVE_FLOW, FTuning.TRAY_SIEVE_FOUL),
    PACKED(FTuning.TRAY_PACKED_EFFICIENCY, FTuning.TRAY_PACKED_FLOW, FTuning.TRAY_PACKED_FOUL);

    /** Share of the vapour reaching the tray that a clean tray holds back. */
    public final float efficiency;
    /** mB a tick a clean tray can take before the rest is carried over to the next. */
    public final int flow;
    /** How fast coke builds on it, relative. */
    public final float foul;

    TrayType(float efficiency, int flow, float foul) {
        this.efficiency = efficiency;
        this.flow = flow;
        this.foul = foul;
    }
}
