// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.engineering;

import io.github.soundgoodizerfan.feedback.modular.ModularTuning;

/**
 * The torque minigame's pure model, shared by the screen (which draws it) and any test: where the green band sits, how wide it is, and
 * how a release at a given torque scores. The gauge runs 0 to 1; the band is centred on {@link #centre} with half width {@link #half}
 * (wider for a better fastener), the thread-strip zone runs {@link ModularTuning#TORQUE_STRIP} past it, and holding past that strips the
 * thread outright. The server never sees these figures: it receives the averaged score and only moves the start condition with it.
 */
public final class Torque {

    /** Words for a pull, by {@link #grade}. */
    public static final int LOOSE = 0, SNUG = 1, PERFECT = 2, OVER = 3, STRIPPED = 4;

    private Torque() {
    }

    /** Where pull {@code pull} of a session seeded {@code seed} wants its torque: moves a little each pull so it cannot be learned by rote. */
    public static float centre(int seed, int pull) {
        int h = (seed * 73856093) ^ (pull * 19349663 + 0x9E3779B9);
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        return 0.56f + 0.18f * ((h & 0xFFFF) / 65535f);
    }

    /** Half the width of the band for a fastener of {@code quality} (1 to 5). */
    public static float half(int quality) {
        return ModularTuning.TORQUE_BAND_BASE + ModularTuning.TORQUE_BAND_PER_QUALITY * Math.max(1, Math.min(5, quality));
    }

    /** The torque at which the thread strips. */
    public static float stripEnd(float centre, float half) {
        return Math.min(1f, centre + half + ModularTuning.TORQUE_STRIP);
    }

    /** The score (0..1) of letting go at {@code torque}; {@code stripped} when the hold ran past the strip zone. */
    public static float score(float torque, float centre, float half, boolean stripped) {
        if (stripped)
            return 0f;
        float lo = centre - half, hi = centre + half;
        if (torque < lo) {
            float u = torque / lo;
            return 0.55f * u * u;
        }
        if (torque <= hi)
            return 0.8f + 0.2f * (1f - Math.abs(torque - centre) / half);
        float d = Math.min(1f, (torque - hi) / ModularTuning.TORQUE_STRIP);
        return 0.5f - 0.35f * d;
    }

    /** The word for a release: {@link #LOOSE}, {@link #SNUG} (in the band, off the middle), {@link #PERFECT}, {@link #OVER} or {@link #STRIPPED}. */
    public static int grade(float torque, float centre, float half, boolean stripped) {
        if (stripped)
            return STRIPPED;
        if (torque < centre - half)
            return LOOSE;
        if (torque <= centre + half)
            return Math.abs(torque - centre) <= half * 0.4f ? PERFECT : SNUG;
        return OVER;
    }

    /** The mean of the first {@code n} pull scores, 0..1. */
    public static float mean(float[] scores, int n) {
        if (n <= 0)
            return 0f;
        float sum = 0f;
        for (int i = 0; i < n; i++)
            sum += scores[i];
        return Math.max(0f, Math.min(1f, sum / n));
    }
}
