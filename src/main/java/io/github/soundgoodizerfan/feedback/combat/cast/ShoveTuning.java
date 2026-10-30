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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
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
