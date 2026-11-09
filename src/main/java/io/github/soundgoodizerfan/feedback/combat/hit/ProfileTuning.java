// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.hit;

/** Reference scales of {@link WeaponProfile}: each axis is a 0 to 1 share between a low and a high figure. Placeholders to be felt in play. */
public final class ProfileTuning {

    private ProfileTuning() {
    }

    /** Burst: joules a single hit carries, shared on a log scale between these. */
    public static final float BURST_LOW_J = 20f, BURST_HIGH_J = 1500f;
    /** Tempo: hits per second between these. */
    public static final float TEMPO_SLOW = 0.5f, TEMPO_FAST = 4f;
    /** Precision of a gun: cone half-angle in degrees, log scale; the tight end scores 1. */
    public static final float SPREAD_TIGHT_DEG = 0.1f, SPREAD_LOOSE_DEG = 8f;
    /** Precision of melee: reach (blocks beyond the reference) and edge hardness that score 1, and the weights of lightness, reach and edge. */
    public static final float REACH_LONG = 1.5f, EDGE_HARD = 8f;
    public static final float PRECISION_LIGHT_W = 0.4f, PRECISION_REACH_W = 0.3f, PRECISION_EDGE_W = 0.3f;
    /** Stealth of a gun: weight of low report against light mass. Of melee: weight of lightness against a dull (soft) edge. */
    public static final float STEALTH_REPORT_W = 0.7f, STEALTH_MELEE_LIGHT_W = 0.6f;
    /** Bows: joules of a full-speed arrow, and the fixed precision and stealth they read. */
    public static final float BOW_BASE_J = 60f, BOW_STEALTH = 0.8f;
    /** Bare hands: joules of a blow, swings per second, base precision, what claws add. */
    public static final float BARE_JOULES = 10f, BARE_SWINGS = 3f, BARE_PRECISION = 0.3f, CLAW_PRECISION = 0.4f;
    /** Stealth: a plate this heavy (Mu) and the most stealth it takes off a hand. */
    public static final float PLATE_LOUD_MU = 1f, PLATE_LOUD_MAX = 0.6f;
    /** Tempo share a weapon (or a bare hand) needs for the tempo skills (Rising tempo, Frenzy). */
    public static final float TEMPO_MIN = 0.5f;
    /** The share from which an axis earns a word on a tooltip. */
    public static final float WORD = 0.6f;
}
