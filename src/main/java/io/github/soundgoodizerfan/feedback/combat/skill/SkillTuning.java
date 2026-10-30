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
package io.github.soundgoodizerfan.feedback.combat.skill;

/**
 * Skill point awards and their diminishing returns. All placeholder figures, tuned by playing.
 * An award is {@code base * DECAY^c}, rounded, where {@code c} is how many recent awards the same
 * target (or parried attacker) already paid; one recent count is forgotten every
 * {@link #FORGET_TICKS}, so a dummy stops paying but a fresh enemy pays in full.
 */
public final class SkillTuning {

    public static final int PARRY = 10, EVASION = 8, GUN_HIT = 2, DISORDER = 8, MELEE_HIT = 3, FIST_HIT = 3, SURVIVED = 4, PUSH = 2, AID = 3, HAZARD = 2, SALVAGE = 3;
    /** Skill points a clean forging streak pays Onslaught, per variant, diminishing like any other target. */
    public static final int FORGE_STREAK = 2;
    /** A gun hit at or past this many blocks pays Observation; nearer ones pay nothing. */
    /** Out-of-combat trickles: a loaded crank click, a tether pull held against crouched, a vibration listener left unwoken ([OPEN] all). */
    public static final int CRANK = 2, HELD_PULL = 1, HUSHED = 2;

    public static final double OBSERVE_RANGE = 6.0;
    /** Fraction of the previous payout each repeat on one target keeps. */
    public static final float DECAY = 0.6f;
    /** Ticks for one recent count to be forgotten (10 s). */
    public static final int FORGET_TICKS = 200;
    /** Highest count a target can build, and how many targets are remembered (oldest dropped). */
    public static final int MAX_COUNT = 8, MAX_RECENT = 12;
    /** Roll distance on flat ground with no skill, blocks: {@code RollTuning.SPEED} over friction. */
    public static final float ROLL_BASE_BLOCKS = 3f;

    private SkillTuning() {
    }
}
