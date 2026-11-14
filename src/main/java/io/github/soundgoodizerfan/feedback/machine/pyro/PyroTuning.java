// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.pyro;

/** Figures for the pyroelectric plate. */
public final class PyroTuning {

    private PyroTuning() {
    }

    /** Ticks between samples of the thermal bodies a plate touches (twice a second). */
    public static final int SAMPLE_INTERVAL_TICKS = 10;

    /** Invented. The finest rate, in Tu per second, a plate of full pyroelectric coupling reads; a weaker material reads this divided by its coupling. */
    public static final float RESOLUTION_TU_PER_S = 0.5f;

    /** Rate, in Tu per second, past which Jade says "fast". */
    public static final float FAST_TU_PER_S = 10f;
}
