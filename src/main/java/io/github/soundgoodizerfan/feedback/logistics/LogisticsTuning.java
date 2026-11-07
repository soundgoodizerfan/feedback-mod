// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics;

/**
 * Every figure the belts and the sorting reader read. All invented: nothing here has been played,
 * so each is safe to retune. Kept beside the code that uses it rather than in {@code FTuning}.
 */
public final class LogisticsTuning {

    private LogisticsTuning() {
    }

    // --- sort bands (Mu/mB, unitless, Tu) ---
    /** Density under this reads light, in Mu per mB. */
    public static final float DENSITY_LIGHT_BELOW = 3f;
    /** Density at or over this reads heavy, in Mu per mB. */
    public static final float DENSITY_HEAVY_FROM = 7f;
    /** Reflectivity under this reads dull. */
    public static final float REFLECT_DULL_BELOW = 0.3f;
    /** Reflectivity at or over this reads bright. */
    public static final float REFLECT_BRIGHT_FROM = 0.6f;
    /** Hardness (St) under this reads soft. */
    public static final float HARDNESS_SOFT_BELOW = 3f;
    /** Hardness (St) at or over this reads hard. */
    public static final float HARDNESS_HARD_FROM = 20f;
    /** Friction coefficient under this reads slick. */
    public static final float FRICTION_SLICK_BELOW = 0.45f;
    /** Friction coefficient at or over this reads grippy. */
    public static final float FRICTION_GRIPPY_FROM = 0.65f;
    /** Temperature (Tu) at or over this reads hot (under it, over the warm line, warm). */
    public static final float TEMP_HOT_FROM = 200f;
    /** Total mass (Mu) of the unit under this reads small. */
    public static final float SIZE_SMALL_BELOW = 1f;
    /** Total mass (Mu) of the unit at or over this reads large. */
    public static final float SIZE_LARGE_FROM = 16f;

    // --- noise ---
    /** Half-width of the uncertain zone around a cut, as a share of the cut value, for a plain detector. */
    public static final float NOISE_MARGIN = 0.25f;
    /** Chance of the wrong fate exactly on a cut (it falls to nothing at the margin's edge). */
    public static final float NOISE_PEAK = 0.5f;
    /** Reflectivity a glowing-hot piece appears to gain to an optical reader. */
    public static final float GLOW_REFLECT_GAIN = 0.35f;
    /** Reflectivity a fouled (dirty) optical head loses to everything it reads. */
    public static final float DIRT_REFLECT_LOSS = 0.2f;
    /** Fraction of its pull a permanent magnet keeps per 100 Tu of its own warmth over ambient (read as extra margin). */
    public static final float MAGNET_WARM_MARGIN = 0.25f;

    // --- belts ---
    /** Belt travel in blocks per tick per RPM of the head roller. */
    public static final float BELT_BLOCKS_PER_TICK_PER_RPM = 0.0015f;
    /** Mass (Mu) one belt block carries before the run's cargo cap is reached: a block holds this much at most. */
    public static final float BELT_MASS_PER_BLOCK = 64f;
    /** Most cargo units per belt block (a bound on ledger size). */
    public static final int BELT_UNITS_PER_BLOCK = 4;
    /** Gap (blocks) between consecutive units of negligible mass. */
    public static final float BELT_MIN_GAP = 0.25f;
    /** Extra gap (blocks) per Mu of the leading unit, capped by {@link #BELT_MAX_GAP}. */
    public static final float BELT_GAP_PER_MU = 0.01f;
    /** Largest gap (blocks) a heavy unit takes. */
    public static final float BELT_MAX_GAP = 0.9f;
    /** Su a bare head roller asks of its shaft. */
    public static final float BELT_BASE_LOAD_SU = 1f;
    /** Su per Mu of carried cargo per block of length. */
    public static final float BELT_LOAD_SU_PER_MU = 0.004f;
    /** Ticks between retries of cargo pinned at a blocked end. */
    public static final int BELT_RETRY_TICKS = 10;
    /** Ticks between client snapshots of a run whose cargo changed. */
    public static final int BELT_SYNC_TICKS = 4;
    /** Entity push (blocks per tick) per belt-block-per-tick of belt speed. */
    public static final float BELT_RIDE_GAIN = 1f;
    /** Condition (share of 1) a belt block loses per second per hot unit on it, per Tu of excess over its limit. */
    public static final float BELT_HOT_WEAR_PER_TU = 0.00002f;
    /** Condition floor of a belt block: worn belts slip, they never fail. */
    public static final float BELT_CONDITION_FLOOR = 0.2f;
    /** Share of full speed a belt at its floor still makes (speed runs linearly from 1 down to this). */
    public static final float BELT_SLIP_SPEED_FLOOR = 0.5f;
    /** Most mass (Mu) one cargo unit holds; a bigger stack is split. */
    public static final float BELT_UNIT_MASS_CAP = 32f;
    /** Ticks between a funnel's pulls from the handler it faces. */
    public static final int BELT_PULL_TICKS = 8;
    /** Blocks a tick a powered linear-motor cell drives its run at (invented; about 80 RPM of a shaft). */
    public static final float BELT_LINEAR_SPEED = 0.12f;
    /** Ticks a band stays wet after a fluid was above it or a fluid-holding item rode it (invented). */
    public static final int WET_DRY_TICKS = 600;
    /** Most ticks of belt travel a run makes up in one jump when its chunk loads again (invented: a minute). */
    public static final int BELT_CATCHUP_MAX_TICKS = 1200;
    /** Units a dirty optical head reads before it needs wiping. */
    public static final int OPTICAL_DIRT_AT = 200;
    /** Added grip coefficient of a cleat. */
    public static final float CLEAT_GRIP = 0.35f;
    /** Friction multiple of a wet belt (a fluid on it). */
    public static final float WET_GRIP = 0.5f;
    /** Friction product (belt x cargo) a rise needs to hold cargo. */
    public static final float INCLINE_TAN = 0.3f;

    // --- modules ---
    /** Drop (blocks of chute) beyond which fragile cargo breaks. */
    public static final int CHUTE_FRAGILE_DROP = 3;
    /** Hardness (St) under which cargo is fragile in a long drop... read as: toughness under this share of hardness. */
    public static final float FRAGILE_TOUGHNESS_SHARE = 0.2f;
    /** Fines (units screened out) a screen takes before it fouls. */
    public static final int SCREEN_FOUL_AT = 24;
    /** Aperture limit (Mu per unit) for fine, medium, coarse screens. */
    public static final float[] SCREEN_APERTURE_MU = {0.5f, 2f, 8f};
    /** Air jet: Pu a nozzle needs to lift anything, and the density (Mu/mB) cut it makes per Pu. */
    public static final float JET_MIN_PU = 1f;
    public static final float JET_DENSITY_PER_PU = 1.5f;
    /** Eu a powered module draws (amps at its band). */
    public static final int POWERED_AMPS = 1;
    /** Tu a fan strips from each cargo unit that passes. */
    public static final float FAN_COOL_TU = 120f;
    /** Tu a cold plate / cooler holds cargo at or above. */
    public static final float COOLER_FLOOR_TU = 60f;
    /** Tu a cooler strips from each unit passing. */
    public static final float COOLER_COOL_TU = 400f;
    /** Ticks a module waits (e.g. gate hold re-check). */
    public static final int MODULE_PULSE_TICKS = 10;
    /** Counter: ticks of passage history averaged for its rate reading. */
    public static final int COUNTER_WINDOW_TICKS = 200;
}
