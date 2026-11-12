// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.engineering;

/**
 * The fitting minigame's pure model: a Simon Says of tool strokes. The table plays a sequence of strokes (file, tap, turn, press) and the
 * player repeats it. What the parts are made of drives the play: harder stock takes more strokes, tougher stock gives longer to read each one
 * and forgives a slip. A wrong stroke ends the play there and the score is how far it got; the score is only the start quality of the
 * assembly, never a gate. Pure arithmetic, shared by the screen and any test.
 */
public final class FittingSequence {

    /** The strokes, by index: file, tap, turn, press. */
    public static final int STROKES = 4;
    /** Fewest and most strokes in a sequence. */
    public static final int MIN_LENGTH = 4, MAX_LENGTH = 12;
    /** Ticks each stroke is shown for, at the fastest and slowest. */
    public static final int FASTEST_TICKS = 9, SLOWEST_TICKS = 20;

    private FittingSequence() {
    }

    /** Hardness (St) at and above which stock counts as fully hard for the length; a soft stock sits near 0. */
    private static final float HARD_REF = 12f;
    /** Toughness (St) at and above which stock counts as fully tough. */
    private static final float TOUGH_REF = 12f;

    /** Strokes in the sequence for {@code pieces} parts of mean hardness {@code hardness}: more parts and harder stock, more strokes. */
    public static int length(int pieces, float hardness) {
        float hard = Math.min(1f, Math.max(0f, hardness / HARD_REF));
        return Math.max(MIN_LENGTH, Math.min(MAX_LENGTH, Math.round(2 + pieces + 3 * hard)));
    }

    /** Ticks each stroke is shown for: tougher stock is slower and easier to read. */
    public static int stepTicks(float toughness) {
        float tough = Math.min(1f, Math.max(0f, toughness / TOUGH_REF));
        return Math.round(FASTEST_TICKS + (SLOWEST_TICKS - FASTEST_TICKS) * tough);
    }

    /** Wrong strokes forgiven before the play ends: one for tough stock, none otherwise. */
    public static int slack(float toughness) {
        return toughness >= TOUGH_REF * 0.6f ? 1 : 0;
    }

    /** The sequence for a seed: {@code length} strokes, each 0..3, never the same stroke three times running. */
    public static int[] strokes(long seed, int length) {
        int[] out = new int[length];
        long s = seed * 6364136223846793005L + 1442695040888963407L;
        for (int i = 0; i < length; i++) {
            s = s * 6364136223846793005L + 1442695040888963407L;
            int v = (int) ((s >>> 33) % STROKES);
            if (i >= 2 && out[i - 1] == v && out[i - 2] == v)
                v = (v + 1) % STROKES;
            out[i] = v;
        }
        return out;
    }

    /** The score of a play that repeated {@code done} of {@code length} strokes right, 0..1; a play that finished is 1. */
    public static float score(int done, int length) {
        return length <= 0 ? 1f : Math.min(1f, Math.max(0f, done / (float) length));
    }
}
