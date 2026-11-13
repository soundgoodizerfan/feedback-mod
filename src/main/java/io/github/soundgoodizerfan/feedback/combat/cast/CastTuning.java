// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.combat.cast;

/** Figures of the reworked casts and riders (Rebuke, Reckoning, Shadow step, Vanish, Smoke step, Shake off, Sear shut, Blood up, Spoor). All [OPEN] stand-ins until played. */
final class CastTuning {

    /** Rebuke: stamina the counter-strike costs, and how far (blocks) the one who struck may stand and still be answered. */
    static final float REBUKE_STAMINA = 15f;
    static final double REBUKE_REACH = 3.5;

    /** Reckoning: the share of the last parried blow's energy that goes back, ticks the last parry stays loaded, and the most it can carry (damage). */
    static final float RECKON_SHARE = 1f, RECKON_MAX = 40f;
    static final int RECKON_TICKS = 100;

    /** Shadow step: blocks the flank covers, speed (blocks per tick), and ticks the Ambush rider stays up after it. */
    static final double FLANK_SIDE = 3.5, FLANK_SPEED = 1.1, FLANK_REACH = 9;
    static final int FLANK_TICKS = 8, AMBUSH_TICKS = 60;

    /** Vanish: highest light (0 to 15) that counts as dark, blocks per tick (squared) that count as still and as a crouched walk, and stamina a held tick costs. */
    static final int VANISH_DARK = 4;
    static final double VANISH_STILL = 0.02 * 0.02, VANISH_CROUCH_WALK = 0.09 * 0.09;
    static final float VANISH_DRAIN = 0.15f;

    /** Smoke step: dust cloud radius (blocks) and ticks it lingers, on firm ground; loose ground (sand, snow, dust) widens and lengthens it by these factors. */
    static final double CLOUD_RADIUS = 2.0, CLOUD_THICK_RADIUS = 1.5;
    static final int CLOUD_TICKS = 40, CLOUD_THICK_TICKS = 40;
    static final float LOOSE_RADIUS = 1.5f, LOOSE_TICKS = 1.5f;
    /** Blocks hard enough that the roll kicks up almost nothing: a puff this small is nearly no cloud. */
    static final float HARD_SHARE = 0.5f;

    /** Shake off: ticks of the shake's relief (it clears fire, a blinding or sticky effect, a stun). */
    static final int SHAKE_STUN_CLEAR = 1;

    /** Sear shut: shares of Cauterize's heat spent, burn paid and rearm wait a practised hand keeps (less of each). */
    static final float SEAR_SHUT_SPENT = 0.5f, SEAR_SHUT_BURN = 0.5f, SEAR_SHUT_REARM = 0.5f;

    /** Blood up: ticks a warm-muscle spell lasts after a clean run of hits, and the stamina regain factor it gives. */
    static final int WARM_TICKS = 160;
    static final float WARM_REGEN = 1.6f;

    /** Spoor: blocks within which tracks are found, ticks they show, and how many ground marks one scan keeps. */
    static final double SPOOR_RANGE = 16;
    static final int SPOOR_TICKS = 1200;

    private CastTuning() {
    }
}
