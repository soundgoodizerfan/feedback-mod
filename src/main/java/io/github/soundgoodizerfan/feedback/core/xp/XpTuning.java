// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.xp;



/**
 * Tuning constants for the core/xp code.
 * Every figure is a placeholder or invented unless its comment says it comes from the slice design.
 */
public final class XpTuning {

    private XpTuning() {
    }
    /** Invented. The sculk culture (roadmap 6.8): mass a fresh seed starts at and a harvest leaves, points of XP one whole mass costs to grow, and the most points a tick growth can take (a whole mass in about 10 minutes when supplied). */
    public static final float CULTURE_START_MASS = 0.1f, CULTURE_HARVEST_MASS = 0.3f, CULTURE_POINTS_PER_MASS = 400f,
            CULTURE_GROWTH_POINTS_PER_TICK = 400f / 12000f;

    /** Invented. Points a tick a full-mass culture eats just to live (one point every 10 s); scales with mass. */
    public static final float CULTURE_UPKEEP_PER_TICK = 0.005f;

    /** Invented. Ticks with no XP before growth stops (dormant, 10 min) and before the mass starts to waste (atrophy, 30 min), and the mass lost a tick once it does (1% per 10 s). */
    public static final int CULTURE_DORMANT_TICKS = 12000, CULTURE_ATROPHY_TICKS = 36000;

    public static final float CULTURE_ATROPHY_PER_TICK = 0.00005f;

    /** Invented. Ticks without air before the mass starts to die (1 min), and the mass lost a tick after (2% per 10 s, dead in about 8 min from full). */
    public static final int CULTURE_AIRLESS_GRACE_TICKS = 1200;

    public static final float CULTURE_SUFFOCATE_PER_TICK = 0.0001f;

    /** Invented. A full culture left alone: ticks before it clots and bleeds XP (10 min), the share of its points lost a tick then (2% per 10 s), and further ticks before it buds a shrieker (20 min). */
    public static final int CULTURE_OVERRIPE_TICKS = 12000, CULTURE_SHRIEKER_DELAY_TICKS = 24000;

    public static final float CULTURE_LEAK_PER_TICK = 0.0001f;

    /** Invented. A ripe culture growing a relay organ: ticks it takes under the right conditions (5 min), the window it can be cut in before it overgrows (3 min; inside the overripe clot at 10 min), and the points a tick it eats on top of upkeep (2 a second). */
    public static final int CULTURE_ORGAN_READY_TICKS = 6000, CULTURE_ORGAN_WINDOW_TICKS = 3600;

    public static final float CULTURE_ORGAN_POINTS_PER_TICK = 0.1f;

    /** Invented. Blocks around an Orb Absorber from which it pulls loose orbs into its run, once a second. */
    public static final double ABSORBER_RADIUS = 4.0;

    /** Invented. Blocks around an Ore Tap and a Trade Tap within which a block broken or a trade made gives its XP to the run. */
    public static final double ORE_TAP_RADIUS = 8.0, TRADE_TAP_RADIUS = 8.0;

    /** Invented. Points a Smelter Tap moves from a vessel's bank to its run each second. */
    public static final int SMELTER_TAP_POINTS_PER_SECOND = 20;

    /** Invented. Share of an attunement's session cost that stripping it at the Disenchanting Stone gives back. */
    public static final float UNATTUNE_REFUND = 0.5f;
}
