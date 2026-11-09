// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.skill;

/**
 * Skill point awards and their diminishing returns. All placeholder figures, tuned by playing.
 * An award is {@code base * DECAY^c}, rounded, where {@code c} is how many recent awards the same
 * target (or parried attacker) already paid; one recent count is forgotten every
 * {@link #FORGET_TICKS}, so a dummy stops paying but a fresh enemy pays in full.
 */
public final class SkillTuning {

    public static final int PARRY = 10, EVASION = 8, GUN_HIT = 2, DISORDER = 8, MELEE_HIT = 3, FIST_HIT = 3, PUSH = 2, AID = 3, HAZARD = 4, EAT_COOKED = 4, SALVAGE = 3;
    /** A repeatable trickle (aid, hazard handling, a cooked meal) stops paying once this many of its recent payouts are unforgotten; one is forgotten every {@link #FORGET_TICKS}. */
    public static final int TRICKLE_CAP = 2;
    /** Sparks that fly off a player each time they gain skill points (tinted by the tree). */
    public static final int GAIN_SPARKS = 8;
    /** Skill points a clean forging streak pays Onslaught, per variant, diminishing like any other target. */
    public static final int FORGE_STREAK = 2;
    /** A gun hit at or past this many blocks pays Observation; nearer ones pay nothing. */
    /** Out-of-combat trickles: a loaded crank click, a tether pull held against crouched, a vibration listener left unwoken ([OPEN] all). */
    public static final int CRANK = 2, HELD_PULL = 1, HUSHED = 2;

    /**
     * Hit payouts by axis ([OPEN] all). {@code HIT}: a landed blow pays the tree of the weapon's strongest of burst, tempo
     * and precision. {@code NATURE_HIT}: it also pays the tree whose kind of swing it was (a blow of at least
     * {@code BRUTE_HIT_J} joules, one landed within {@code CHAIN_TICKS} of the last, a placed hit). {@code STEALTH_HIT} times
     * the weapon's stealth share is what a blow on an unaware creature pays Veil. A melee blow at {@code REACH_TIP} of the
     * reach or beyond lands at the tip.
     */
    public static final int HIT = 3, NATURE_HIT = 3, STEALTH_HIT = 4, CHAIN_TICKS = 30;
    public static final float BRUTE_HIT_J = 50f, REACH_TIP = 0.75f;

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
