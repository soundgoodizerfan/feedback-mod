// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.glow;

/** Figures for a block of glass that shows radiation. */
public final class GlowTuning {

    private GlowTuning() {
    }

    /** Brightest it gets: faint glass, never a lamp. */
    public static final int MAX_GLOW = 8;

    /** Invented. The dose, in Ru, at which each glow level starts (level 1 at the first, 8 at the last); below the first it is dark. */
    public static final float[] BANDS_RU = {0.02f, 0.1f, 0.3f, 1f, 3f, 10f, 30f, 100f};

    /** Ticks between looks at the dose (two seconds). */
    public static final int SAMPLE_INTERVAL_TICKS = 40;

    /** Side, in blocks, of a cell sharing one dose reading, and ticks a reading is kept. */
    public static final int CELL_BLOCKS = 4;
    public static final long CACHE_TICKS = 40;

    /** The glow level for a dose: 0 below the first band. */
    public static int levelFor(float ru) {
        int level = 0;
        for (int i = 0; i < BANDS_RU.length && ru >= BANDS_RU[i]; i++)
            level = i + 1;
        return level;
    }
}
