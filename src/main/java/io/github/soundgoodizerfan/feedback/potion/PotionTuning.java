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
package io.github.soundgoodizerfan.feedback.potion;

import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;

/**
 * Placeholder figures for the brewing stand (potions spec 3, 12.6, 12.7), all tuned in play. Heat is in Tu; with water in the vessel
 * the stand pins at boiling, like the stockpot, so the working range is ambient to 100.
 */
public final class PotionTuning {

    private PotionTuning() {}

    /** Volume of one bottle, mB (spec 12.7); a stand holds three bottles' worth of water and dilutes nothing. */
    public static final int BOTTLE_MB = 250, BOTTLES = 3;

    /** How much surface a cut leaves (index = {@code FoodCut.level}): whole, sliced, minced. Surface sets how fast a compound comes out. */
    public static final float[] GRIND = {0.3f, 0.6f, 1.0f};
    /** The flask's own size on the map: it touches a zone or vortex when its edge does (a node needs its centre inside the ring), so contact is this much further out than the feature's radius. The screen draws the flask this wide and scales it with the map. */
    public static final float MARKER_RADIUS = 0.06f;
    /** Under this extraction stalls; at {@link #COOK_TU} it is full. A node will not cook in below {@link #COOK_TU}. */
    public static final float COLD_TU = 30f, COOK_TU = 70f;
    /** Distance on the plane a full-pace stir moves the marker in a tick, and the shortest a path segment counts as for progress. */
    public static final float STIR_DIST_PER_TICK = 0.008f, MIN_SEGMENT = 0.05f;
    /**
     * Potency lost inside a precipitate or scorch zone: a little for each tick spent in it, and more for each unit of path the marker covers
     * there, which a fast stir (pace 1) hits twice as hard as a still one. So a slow stir spends less path in a zone and loses less to it, and
     * a fast one covers ground at a price. Potency recovers outside a zone.
     */
    public static final float DRAIN_STILL_PER_TICK = 0.0003f, DRAIN_PER_DISTANCE = 0.35f, FAST_HIT = 1f, RECOVER_PER_TICK = 0.002f;
    /** A ladle of water sends the marker this far straight toward the centre (or home, if nearer) and costs this much potency. */
    public static final float LADLE_STEP = 0.15f, LADLE_POTENCY = 0.1f;
    /** Fog: the map is a grid of cells over this half-width; the marker uncovers cells within the reveal radius as it passes. */
    public static final float FOG_EXTENT = 1.2f, FOG_REVEAL = 0.15f;
    public static final int FOG_N = 40;
    /** Over this a vortex spins; it turns a marker inside it this many radians a tick, and a marker inside the core fraction of its radius jumps. */
    public static final float VORTEX_TU = 80f, VORTEX_TURN_PER_TICK = 0.03f, VORTEX_CORE = 0.3f;
    /** A rotten plant gives up this much of its reach at full rot, and drifts toward the nearest precipitate zone by this fraction of the reach it had. */
    public static final float ROT_LOSS = 0.8f, ROT_PULL = 0.4f;
    /** A stabiliser only acts on a near miss: within this many node radii. */
    public static final float PULL_REACH_RADII = 2.5f;
    /** Strength of a node's centre against its edge in duration: a centred duration is full, an edge one is this fraction of it. */
    public static final float DURATION_AT_EDGE = 0.35f;
    /** Ticks for a potion's potency to halve at room temperature (3 game days); under {@link #MIN_POTENCY} it has no effect. */
    public static final float HALF_LIFE_TICKS = 72000f;
    public static final float MIN_POTENCY = 0.1f;
    /** The plain water colour a dud draught with no class to lean on shows. */
    public static final int WATER_COLOR = 0x3F76E4;
    /** A dud draught's nausea, ticks. */
    public static final int DUD_NAUSEA_TICKS = 200;
    /** Ticks the stirring input holds after the last packet. */
    public static final int STIR_HOLD_TICKS = 10;
    /** Relax rate of the stand toward its fire per tick. */
    public static final float RELAX_PER_TICK = 0.02f;

    /**
     * Throwing (spec 5), [OPEN] until played. A bottle's volume spreads over a ball of this many times the radius it fills as liquid (a 250 mB
     * bottle reaches about 2.5 blocks); a thrown length is this fraction of a drunk one before distance takes its share.
     */
    public static final float THROW_SPREAD = 4f, THROW_DURATION = 0.75f;
    /** The explosion node's heat: damage per level plus one at the centre, falling off to nothing at the reach, and seconds alight at the centre. */
    public static final float BLAST_DAMAGE = 4f, BLAST_BURN_SECONDS = 4f;
    /** Drunk or eaten explosion: the thrown damage and burn at the centre times this ([OPEN] until played): 10 at level I, 30 at III, against 20 health. */
    public static final float DRINK_BLAST_FACTOR = 2.5f;
    /** Imbue (spec 12.5), [OPEN] until played: the rider is the potion's level divided by this, a fraction of its length capped at a number of ticks. The soak's hits are the dip's, times the node's {@code imbue_hits} (poison 2, [OPEN] until played). */
    public static final int IMBUE_LEVEL_DIVISOR = 2, IMBUE_MAX_TICKS = 160;
    public static final float IMBUE_DURATION_FRACTION = 0.2f;

    /**
     * Potion salts (spec 4, 12.6, 14.8), [OPEN] until played. Dryer: the chamber must be at least {@code DRYER_MIN_TU} and at or under {@code DRYER_MAX_PU}
     * (gauge, negative is vacuum) for {@code DRYER_TICKS} ticks; the pump pulls {@code DRYER_PU_PER_RPM} Pu a tick of target per RPM down to {@code DRYER_FULL_PU}
     * and the chamber follows its target at {@code DRYER_RELAX_PER_TICK}. Over {@code DRYER_SCORCH_TU} the salt scorches, losing {@code DRYER_SCORCH_PER_TU_TICK}
     * of its potency per Tu over per tick (capped at {@code DRYER_SCORCH_MAX}); over {@code DRYER_CRACK_TU} under vacuum the bottle cracks, {@code DRYER_CRACK_CHANCE} a tick.
     */
    public static final Tu DRYER_MIN_TU = new Tu(60f), DRYER_SCORCH_TU = new Tu(110f), DRYER_CRACK_TU = new Tu(200f);
    public static final Pu DRYER_MAX_PU = new Pu(-0.5f), DRYER_FULL_PU = new Pu(-0.9f);
    public static final float DRYER_PU_PER_RPM = 0.01f, DRYER_RELAX_PER_TICK = 0.02f;
    public static final int DRYER_TICKS = 200;
    public static final float DRYER_SCORCH_PER_TU_TICK = 0.00005f, DRYER_SCORCH_MAX = 0.8f, DRYER_CRACK_CHANCE = 0.02f;
    public static final Su DRYER_LOAD_SU = new Su(20f);
    public static final ThermalMass DRYER_MASS = new ThermalMass(30f);
    public static final Conductance DRYER_LEAK = new Conductance(0.05f);
    /** Reconstituting keeps this fraction of the salt's potency, plus or minus half of this spread at random (the noise floor). */
    public static final float SALT_KEEP = 0.7f, SALT_KEEP_SPREAD = 0.1f;

    /**
     * Taste lacing (spec 7, 14.9), [OPEN] until played. A laced potion is noticed when its strength (potency shared over the servings) is at
     * least {@code TASTE_MIN_STRENGTH} and its taste lies further than {@code TASTE_NOTICE_DISTANCE} from the food's own taste (tastes are
     * about unit length, so the distance runs 0 to about 1.7).
     */
    public static final float TASTE_MIN_STRENGTH = 0.2f, TASTE_NOTICE_DISTANCE = 0.8f;

    /**
     * Fine palate, [OPEN] until played. Both taste thresholds are multiplied by {@code 1 - PALATE_EASE} (a fraction, so a faint or
     * food-like laced dish can still pass unnoticed). Cost: plain eating leaves {@code PALATE_SATURATION_LOSS} of the saturation off.
     */
    public static final float PALATE_EASE = 0.4f, PALATE_SATURATION_LOSS = 0.25f;

    /**
     * Second wind (stimulant cause), [OPEN] until played. Stamina regen is raised by this fraction per level and the pause after a spend is this
     * fraction of its length; when it ends, winded stalls regen for this many ticks per level.
     */
    public static final float SECOND_WIND_REGEN = 0.75f, SECOND_WIND_DELAY = 0.5f;
    public static final int WINDED_TICKS = 160;
    /** Assay (alkaloid cause): stamina regen is this fraction of itself while it lasts (fine focus costs breadth). [OPEN] until played. */
    public static final float ASSAY_REGEN = 0.8f;
    /** Adrenaline: a cast costs this fraction of its stamina while it lasts (healing is off). [OPEN] until played. */
    public static final float ADRENALINE_COST = 0.5f;
    /** Steady hand: spread and camera kick are this fraction of themselves while it lasts, never nothing. [OPEN] until played. */
    public static final float STEADY_HAND_FRACTION = 0.5f;
    /** Quickdraw: reload time falls by this much per level (level I is 0.8x), never under the floor; the crash is mining fatigue this many ticks per level. [OPEN] until played. */
    public static final float QUICKDRAW_PER_LEVEL = 0.2f, QUICKDRAW_FLOOR = 0.5f;
    public static final int QUICKDRAW_CRASH_TICKS = 160;
    /**
     * Heat ward (fire resistance's trade), [OPEN] until played. Cold damage while it lasts is multiplied by COLD_DAMAGE; frost builds one extra tick
     * every COLD_BUILD_EVERY ticks in powder snow (1.5x with 2). Vanilla freezing is the only cold hazard the mod has.
     */
    public static final float HEAT_WARD_COLD_DAMAGE = 1.5f;
    public static final int HEAT_WARD_COLD_BUILD_EVERY = 2;
    /** Antivenom: this fraction of poison tick damage passes; stamina regen runs at REGEN while it lasts (a binder thickens the blood). [OPEN] until played. */
    public static final float ANTIVENOM_PASS = 0.4f, ANTIVENOM_REGEN = 0.7f;
    /** Smelter's blood: bare skin holds this many Tu hotter (a ceiling raise only; hotter pieces still burn); swing and dig speed fall by SLOW (dulled hand). [OPEN] until played. */
    public static final float SMELTERS_BLOOD_TU = 150f, SMELTERS_BLOOD_SLOW = 0.15f;
    /**
     * Radiation ward (protein with sulphur, a radical scavenger), [OPEN] until played. This fraction of the radiation dose taken each interval passes (it
     * binds radicals, it does not stop the rays, so never below about a half); healing runs at HEAL while it lasts (the scavenger spends the body's repair).
     */
    public static final float RADIATION_WARD_PASS = 0.55f, RADIATION_WARD_HEAL = 0.6f;
    /**
     * Static ground (mineral cause), [OPEN] until played. This fraction of shock damage passes. The trade: every STATIC_GROUND_EVERY ticks a
     * held metal item gains GAIN Tu, up to CAP Tu (the body is a conductor and warms what it holds; the item cools at its usual rate).
     */
    public static final float STATIC_GROUND_PASS = 0.4f, STATIC_GROUND_GAIN = 50f, STATIC_GROUND_CAP = 120f;
    public static final int STATIC_GROUND_EVERY = 20;
    /**
     * Quench (volatile oil cause), [OPEN] until played. Every QUENCH_EVERY ticks, thermal block entities within QUENCH_RADIUS blocks leak
     * QUENCH_COOL_MULT times their usual leak and hot pieces in the pack cool that many times as fast; frost builds one extra tick every
     * QUENCH_COLD_BUILD_EVERY ticks in powder snow (1.33x with 3).
     */
    public static final int QUENCH_EVERY = 10, QUENCH_COLD_BUILD_EVERY = 3;
    public static final float QUENCH_RADIUS = 5f, QUENCH_COOL_MULT = 4f;
    /** How far a thrown bottle reaches, blocks: the radius of a bottle's liquid, spread. */
    public static float throwReach() {
        return THROW_SPREAD * (float) Math.cbrt(BOTTLE_MB / 1000f);
    }
}
