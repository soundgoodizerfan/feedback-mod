// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.column;


/** The discrete tray a player chooses (spec 6.4): sharper cuts against flow against how fast it cokes. */
public enum TrayType {
    BUBBLE_CAP(PetrochemTuning.TRAY_BUBBLE_CAP_EFFICIENCY, PetrochemTuning.TRAY_BUBBLE_CAP_FLOW, PetrochemTuning.TRAY_BUBBLE_CAP_FOUL),
    SIEVE(PetrochemTuning.TRAY_SIEVE_EFFICIENCY, PetrochemTuning.TRAY_SIEVE_FLOW, PetrochemTuning.TRAY_SIEVE_FOUL),
    PACKED(PetrochemTuning.TRAY_PACKED_EFFICIENCY, PetrochemTuning.TRAY_PACKED_FLOW, PetrochemTuning.TRAY_PACKED_FOUL);

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
