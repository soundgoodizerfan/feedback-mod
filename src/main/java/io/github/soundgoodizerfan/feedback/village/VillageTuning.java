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
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.village;

/**
 * Figures the village economy reads, kept beside the code that uses them (the {@code ModularTuning}
 * pattern). Every number is a starting value to be tuned by play.
 */
public final class VillageTuning {

    private VillageTuning() {
    }

    /** Orders a villager holds: this plus its trade level. */
    public static final int ORDERS_BASE = 2;
    /** A day, in game ticks; orders and stock refresh once a day. */
    public static final long DAY_TICKS = 24000L;

    /** Closeness (0 to 1) at which a delivery reaches Poor, Fair, Good and Exceptional; under the first it is Rejected. */
    public static final float[] CLASS_CUTS = {0.35f, 0.55f, 0.75f, 0.9f};
    /** Pay for each class (Rejected to Exceptional) against the order's base. */
    public static final float[] CLASS_PAY = {0f, 0.4f, 0.7f, 1f, 1.4f};
    /** Villager experience for each class. */
    public static final int[] CLASS_XP = {0, 2, 4, 7, 12};
    /** Gossip added for each class: major positive for Good and Exceptional, minor negative for Rejected. */
    public static final int GOSSIP_GOOD = 3, GOSSIP_EXCEPTIONAL = 8, GOSSIP_REJECTED = 2;
    /** Things a villager wants to buy on a day: least and most lines drawn from its pools. */
    public static final int WANTS_MIN = 4, WANTS_MAX = 6;
    /** Villager experience a purchase or sale gives. */
    public static final int BUY_XP = 1;

    /** Half-width of a band term, as a share of the figure's range: novice, then less each level, to a floor. */
    public static final float TOLERANCE_NOVICE = 0.36f, TOLERANCE_STEP = 0.06f, TOLERANCE_FLOOR = 0.14f;
    /** How far under a must the figure may fall, as a share of the range, before that term scores nothing. */
    public static final float MUST_SLACK = 0.3f;
    /** A biome's lean counts for this much against a stated term's one (stated carries the most, lean next, quirk least). */
    public static final float LEAN_WEIGHT = 0.75f;
    /** A hidden quirk counts for this much against a stated term's one. */
    public static final float QUIRK_WEIGHT = 0.5f;
    /** A named material's stand-in scores at most this (same kind and near stats); only the named material scores 1. */
    public static final float SUBSTITUTE_CAP = 0.8f;
    /** Hardest material a villager may ask for, by trade level 1 to 5 (hardness is the stand-in for how far along the materials are: copper and bronze, then iron, steel, then gems and netherite). */
    public static final float[] ASK_HARDNESS_CAP = {5f, 12f, 20f, 40f, Float.MAX_VALUE};
    /** Share of tool-order asks that name a material class (its kind) instead of one material. */
    public static final float CLASS_ASK_SHARE = 0.35f;
    /** Least miss (weight times shortfall) the villager bothers to name. */
    public static final float COMPLAINT_FLOOR = 0.05f;

    /** Pay lost for each recent delivery in the same bin (type and material kind). */
    public static final float SATURATION_STEP = 0.25f;
    /** Days a bin takes to drain completely. */
    public static final float SATURATION_DRAIN_DAYS = 3f;

    /** Standing (vanilla reputation) at which a villager calls you known, trusted and honoured. */
    public static final int[] STANDING_TIERS = {10, 30, 60};
    /** Price falls this share per point of standing, to a cap; below zero it rises, to its own cap. */
    public static final float PRICE_PER_STANDING = 0.003f, PRICE_DISCOUNT_CAP = 0.3f, PRICE_MARKUP_CAP = 0.5f;

    /** Blocks within which the screen stays open. */
    public static final double REACH = 8.0;

    // --- walk-in shop (Counter) ---

    /** Share of the line price a want sold by hand-over pays: the villager did not choose to come. */
    public static final float PUSH_RATE = 0.75f;
    /** A walk-in sale adds a point of gossip toward the keeper every this many sales. */
    public static final int WALKIN_GOSSIP_EVERY = 4;
    /** Ticks between catchment scans of a stocked Counter. */
    public static final int PULL_SCAN_TICKS = 100;
    /** Ticks between checks on villagers already walking to a Counter. */
    public static final int ARRIVAL_TICKS = 10;
    /** Straight reach of a Counter's pull, in blocks. */
    public static final int PULL_RADIUS = 12;
    /** Villagers considered, and paths asked for, per scan. */
    public static final int PULL_MAX_PER_SCAN = 24, PULL_PATHS_PER_SCAN = 4;
    /** Ticks a pull stays set on a villager before it clears itself. */
    public static final int PULL_EXPIRY = 600;
    /** Walk speed modifier given to the pulled villager. */
    public static final float PULL_SPEED = 0.5f;
    /** Blocks from the front face at which a villager counts as arrived. */
    public static final double ARRIVE_DIST = 2.0;

    /** Errands a villager makes in a day: 1 + level / 2. */
    public static int errandsPerDay(int level) {
        return 1 + level / 2;
    }

    // --- player trade and gouging ---

    /** A villager buys above market up to this multiple (never less than +1 emerald). */
    public static final float GOUGE_CEILING = 1.5f;
    /** Chance per emerald over market of a minor-negative gossip against the keeper. */
    public static final float GOUGE_REP_CHANCE = 0.25f;
    /** A sale under market gives one point of positive gossip every this many sales. */
    public static final int FAIR_REP_EVERY = 4;

    // --- regional trade and freshness ---

    /** The range a regional price multiplier is held to. */
    public static final float REGIONAL_MIN = 0.6f, REGIONAL_MAX = 1.6f;
    /** Share of the price a stale food (going off) earns; a rotten one is refused. */
    public static final float STALE_FACTOR = 0.5f;
    /** Share of a nearby hot body's excess heat that reaches a shelf (the rest is the shelf's own distance and wood). */
    public static final float SHELF_HEAT_SHARE = 0.15f;
    /** Days between purchases of a weekly line. */
    public static final int WEEK_DAYS = 7;

    // --- market day ---

    /** Days between market days (an octal week). */
    public static final int MARKET_PERIOD = 8;
    /** Open road tiles and, among them, interior tiles (a square, not a street) a Market Cross needs to be active. */
    public static final int MARKET_PLAZA_MIN = 25, MARKET_INTERIOR_MIN = 9;
    /** Blocks around an active cross whose villagers and Counters feel market day. */
    public static final int MARKET_REACH = 48;
    /** Extra wants a villager draws from the market pool on market day. */
    public static final int MARKET_EXTRA_WANTS = 3;
    /** Day-stock of a shop line is this many times its usual on market day. */
    public static final int MARKET_STOCK_MULT = 2;
    /** Ticks between rechecks of whether a cross's plaza still stands. */
    public static final int MARKET_RECHECK_TICKS = 200;

    // --- guilds ---

    /** Blocks within which job sites of one profession count as one guild, and the members it takes. */
    public static final int GUILD_RADIUS = 16, GUILD_MIN = 3;
    /** The guild order's pay against the same order from a single villager, plus this much more for each tier above the first. */
    public static final float GUILD_PAY_MULT = 1.5f, GUILD_TIER_PAY = 0.1f;
    /** Share of a member's delivery experience each guild-mate earns, and the most a member may be given in a day. */
    public static final float MENTOR_SHARE = 0.25f;
    public static final int MENTOR_CAP_PER_DAY = 6;

    // --- the ledger board ---

    /** Blocks from a board (or a bell near it) within which villagers count toward the village. */
    public static final int VILLAGE_RADIUS = 48;
    /** Villagers read for standing, and listed for orders, at most. */
    public static final int STANDING_SAMPLE_MAX = 24, BOARD_VILLAGERS_MAX = 24;
    /** Days a contract stays open after it is posted. */
    public static final int CONTRACT_DAYS = 3;
    /** Pieces a contract asks, least and most. */
    public static final int CONTRACT_MIN = 4, CONTRACT_MAX = 16;
    /** Emeralds a finished contract adds, per standing tier above none (1 + tier times this). */
    public static final int CONTRACT_BONUS = 8;
    /** Gossip a finished contract gives every villager in range toward whoever finished it (major positive). */
    public static final int CONTRACT_GOSSIP = 2;
    /** Blocks within which a Counter fills the contracts of a board. */
    public static final int LINK_REACH = 12;

    // --- roads ---

    /** Tiles of road a flood from the front face may walk, and how many it may hold. */
    public static final int ROAD_REACH = 48, ROAD_FLOOD_MAX = 600;
    /** Blocks from a reached road tile a villager still counts as in the catchment. */
    public static final int ROAD_MARGIN = 3;
    /** Ticks between road floods of one Counter. */
    public static final int ROAD_RESCAN_TICKS = 1200;
}
