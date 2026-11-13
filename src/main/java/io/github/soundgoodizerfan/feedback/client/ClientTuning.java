// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.client;

import io.github.soundgoodizerfan.feedback.core.unit.Rpm;

/**
 * Tuning constants for the client code.
 * Every figure is a placeholder or invented unless its comment says it comes from the slice design.
 */
public final class ClientTuning {

    private ClientTuning() {
    }
    /** Invented. The smoke coat above which a tooltip says Smoked. */
    public static final float SMOKE_SHOWN_AT = 0.1f;

    /** Under this level an XP container reads as holding a low level. Invented. [OPEN] until played */
    public static final float XP_LEVEL_LOW_BELOW = 5f;

    /** From this level an XP container reads as holding a high level; between is moderate. Invented. */
    public static final float XP_LEVEL_HIGH_FROM = 15f;

    /** From this share of its ceiling a container reads as nearly full (full only at the ceiling). Invented. */
    public static final float XP_FILL_NEARLY_FROM = 0.9f;


    // --- readouts -------------------------------------------------------------------------

    // Where the free, qualitative speed bands fall. Philosophy 10 says a player standing next to
    // a shaft can see roughly how fast it is going and nothing more precise than that, so these
    // are the only speed figures the game exposes without an instrument -- as words.
    //
    // The boundaries are set against what beat 1 can actually produce: a water wheel makes 4 RPM
    // per flowing side (4 to 16), a hand crank 32. So a one-sided wheel reads slow, a well sited
    // one reads as turning, and only the crank spins fast. Bands that all collapsed onto one
    // adjective would tell the player nothing.
    /** Below this a turning run reads as "turning slowly". */
    public static final Rpm TURNING_RPM = new Rpm(8f);

    /** At or above this a run reads as "spinning fast". */
    public static final Rpm SPINNING_FAST_RPM = new Rpm(24f);

    /** Kick grows this much per shot of a streak, up to {@link #GUN_KICK_CLIMB_MAX} shots. */
    public static final float GUN_KICK_CLIMB_PER_SHOT = 0.12f;

    public static final int GUN_KICK_CLIMB_MAX = 10;

    public static final float GUN_FLINT_MISFIRE = 0.12f;

    public static final int GUN_KICK_MS = 90;
}
