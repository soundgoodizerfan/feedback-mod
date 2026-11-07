// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

/** Shove and Carry's figures. All [OPEN] stand-ins until played; Mu is {@code BodyMass}'s unit, which Sable's body mass is read in as well. */
final class ShoveTuning {

    /** Blocks from the eye the hands reach to take hold, and the slack past it before an over-stretched hold lets go. */
    static final double REACH = 3.0, STRETCH = 1.5;
    /** Lightest body that can be shoved or carried, Mu: a scrap is not a vehicle. */
    static final double MIN_MASS = 20;
    /** Base carry ratio (body mass over yours): one body of your own weight. The Heavy lift and Deadlift nodes add to it. */
    static final double CARRY_RATIO = 1.0;
    /** A carried body hangs this many blocks in front of the camera. */
    static final double HOLD_DISTANCE = 2.0;
    /** Carry: desired speed per block of error, m/s per block, capped at {@link #CARRY_MAX_SPEED}; past {@link #SNAG_ERROR} blocks off the hold the body is snagged and drops. */
    static final double CARRY_GAIN = 6, CARRY_MAX_SPEED = 8, SNAG_ERROR = 4;
    /** Stamina per tick to hold up one body-ratio of weight (a body of your own weight costs this much). */
    static final float CARRY_STAMINA_PER_RATIO = 0.1f;
    /** Stamina per tick just for holding on. */
    static final float HOLD_DRAIN = 0.05f;
    /** Metres per second a movement-speed attribute point walks at (0.1 walks at 4.3). */
    static final double MS_PER_SPEED = 43;
    /** Seconds one tick of effort stands for: a shove delivers your momentum (mass times speed) as an impulse per second of effort. */
    static final double TICK_SECONDS = 0.05;
    /** Safety clamp on one tick's impulse, N s, so a stalled solver can never be handed a huge one. */
    static final double MAX_IMPULSE = 2000;
    /** Stamina per joule of work the push delivers (force times the distance the hand moves), capped per tick. */
    static final double STAMINA_PER_WORK = 0.01, MAX_STAMINA_PER_TICK = 1.5;
    /** Joules of work that pay Brute one point (decaying per body, see SkillTuning). */
    static final double WORK_PER_POINT = 600;

    private ShoveTuning() {
    }
}
