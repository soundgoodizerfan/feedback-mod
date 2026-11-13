// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.engineering;

import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;

/**
 * The rivet minigame's pure model, shared by the screen (which draws it) and any test. A hot rivet sits in the hole and the player holds
 * to swing the hammer up and lets go to strike: the force (0 to 1) must upset the shank into a tight head. Too little leaves the head
 * loose, too much flattens it past the hole and splits it. The metal's hardness drives the feel: a soft rivet (lead, copper) is forgiving
 * and goes tight at a light blow, a hard one (steel, titanium) wants a harder blow in a narrower band. A better fastener (quality) widens
 * the band. The server never sees these figures: it receives the averaged score and only moves the start condition with it.
 */
public final class Upset {

    /** Words for a strike, by {@link #grade}. */
    public static final int LOOSE = 0, TIGHT = 1, PERFECT = 2, FLAT = 3, SPLIT = 4;

    private Upset() {
    }

    /** How hard the metal is, 0 (lead) to 1 (steel and beyond), on a log scale of its hardness. */
    public static float hardShare(float hardness) {
        if (hardness <= 0.3f)
            return 0f;
        return Math.min(1f, (float) Math.log10(hardness / 0.3f) / 2f);
    }

    /** Where strike {@code strike} of a session seeded {@code seed} wants its force: a seeded wander, pushed up by how hard the metal is. */
    public static float centre(int seed, int strike, float hardShare) {
        int h = (seed * 73856093) ^ (strike * 19349663 + 0x9E3779B9);
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        return 0.42f + 0.16f * ((h & 0xFFFF) / 65535f) + ModularTuning.UPSET_HARD_SHIFT * hardShare;
    }

    /** Half the width of the band for a fastener of {@code quality} (1 to 5) in a metal of {@code hardShare}: soft is wide, hard narrow. */
    public static float half(int quality, float hardShare) {
        float base = ModularTuning.UPSET_BAND_BASE + ModularTuning.UPSET_BAND_PER_QUALITY * Math.max(1, Math.min(5, quality));
        return base * (ModularTuning.UPSET_SOFT_MULT - (ModularTuning.UPSET_SOFT_MULT - ModularTuning.UPSET_HARD_MULT) * hardShare);
    }

    /** The force at which the head splits. */
    public static float splitEnd(float centre, float half) {
        return Math.min(1f, centre + half + ModularTuning.UPSET_SPLIT);
    }

    /** The score (0..1) of a strike at {@code force}; {@code split} when the hold ran past the split point. */
    public static float score(float force, float centre, float half, boolean split) {
        if (split)
            return 0f;
        float lo = centre - half, hi = centre + half;
        if (force < lo) {
            float u = force / lo;
            return 0.55f * u * u;
        }
        if (force <= hi)
            return 0.8f + 0.2f * (1f - Math.abs(force - centre) / half);
        float d = Math.min(1f, (force - hi) / ModularTuning.UPSET_SPLIT);
        return 0.5f - 0.35f * d;
    }

    /** The word for a strike: {@link #LOOSE}, {@link #TIGHT}, {@link #PERFECT}, {@link #FLAT} or {@link #SPLIT}. */
    public static int grade(float force, float centre, float half, boolean split) {
        if (split)
            return SPLIT;
        if (force < centre - half)
            return LOOSE;
        if (force <= centre + half)
            return Math.abs(force - centre) <= half * 0.4f ? PERFECT : TIGHT;
        return FLAT;
    }

    /** The mean of the first {@code n} strike scores, 0..1. */
    public static float mean(float[] scores, int n) {
        if (n <= 0)
            return 0f;
        float sum = 0f;
        for (int i = 0; i < n; i++)
            sum += scores[i];
        return Math.max(0f, Math.min(1f, sum / n));
    }
}
