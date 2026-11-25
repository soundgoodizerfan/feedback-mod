// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.space;

/** Placeholders for the Gap crossing: tuned by play. */
public final class SpaceTuning {
    private SpaceTuning() {
    }

    /** Where a body arrives in the Gap: well above its floor, near the middle of the height range. */
    /** Blocks per tick a person is pushed off a wall they touch in the Gap by a jump press (the same as a push-off a hull plate gives). */
    public static final double WALL_PUSH_OFF = 0.42;
    /** Ticks a cabin may go unbreathed before the air its holes bled over the gap is taken off. */
    public static final int CABIN_UNATTENDED_TICKS = 40;
    public static final double ENTRY_Y = 64;
    /** A body in the Gap below this returns to the Overworld. */
    public static final double FLOOR_Y = 0;
    /** Where a body arrives in the Overworld: where the air is thin but not yet zero, so it does not go straight back up. */
    public static final double RETURN_Y = 300;
    /** Ticks a person who falls out of the Gap on no body burns on arrival (10 s). */
    public static final int REENTRY_FIRE_TICKS = 200;
    /** Ticks a body must sit in air of density zero before it leaves the atmosphere. */
    public static final int HOLD_TICKS = 20;
    /** The trigger looks at bodies this often, in ticks. */
    public static final int CHECK_INTERVAL = 5;
    /** A body in the Gap slower than Sable's sleep threshold (0.15 b/s) but faster than this is woken each tick so it keeps coasting. */
    public static final double DRIFT_MIN_SPEED = 0.005;
    /** Scale a body takes in the Gap (1 outside); 1/16 is the least the person's size attribute allows, so people and craft shrink together: its speeds and distances shrink with it, so a long trip is short in blocks and takes the same time. */
    public static final double GAP_SCALE = 0.0625;
    /** The End's bearing in the Gap, degrees round the vertical (0 is the way the Gap's sky shows north-facing, see {@code GapSky}) and above the horizon. The gas giant is drawn on it. */
    public static final float END_YAW_DEG = 265f, END_PITCH_DEG = 4f;
    /** Blocks, in the Gap's own (scaled) size, from the Gap's origin point ({@code 0, ENTRY_Y, 0}) to the End along its bearing: about four minutes at three blocks a second (a full-size 50 a second). A guess: only play sets it. */
    public static final double END_DISTANCE = 750;
    /** A body within this many Gap blocks of the End, heading toward it, is taken in. The wider it is, the more forgiving the heading. */
    public static final double END_CAPTURE_RADIUS = 90;
    /** Height a craft arrives at in the End, blocks: inside the thin air, under where the air is none. */
    public static final double END_ARRIVE_Y = 280;
    /** Share of where the craft entered the capture sphere (its full-size equivalent) that it is off the main island by on arrival: aim matters a little. */
    public static final double END_OFFSET_SHARE = 0.1;
    /** Degrees below the horizon a craft's speed is turned to on arrival: it comes in over the gas sea and descends. */
    public static final double END_DESCENT_DEG = 10;
    /** Blocks short of the End (along its bearing, outside the capture sphere) where a craft arrives back in the Gap. */
    public static final double END_RETURN_CLEARANCE = 12;
    /** Ticks between looks at the heat on a fast body's leading face. */
    public static final int REENTRY_INTERVAL_TICKS = 20;
    /** Air density times speed cubed (full-size blocks a second) under which a body does not heat: aircraft speeds in thick air, and slow passes in thin. Placeholder. */
    public static final double REENTRY_FLUX_FLOOR = 1.0e6;
    /** Tu of wall temperature for each unit of that flux above the floor. Placeholder: a metal that fails at 1500 Tu (iron melts there) goes at a flux of about 1.6e7. */
    public static final double REENTRY_TU_PER_FLUX = 1.0e-4;
    /** Full-size blocks a second a body's speed must change in one physics step to count as a hit; no engine does it. */
    public static final double IMPACT_MIN_SPEED_CHANGE = 8;
    /** Blocks of fall (of the fall that would stop a person as fast) that hurt nobody: vanilla's three. */
    public static final double IMPACT_FREE_FALL_BLOCKS = 3;
    /** The fall, in blocks, that a hit must be worth before it tears gas cells aboard (the chance is nothing here and certain at twice it). */
    public static final double IMPACT_PUNCTURE_FALL_BLOCKS = 6;
    /** Ticks the crossing overlay takes to clear. */
    public static final int FADE_TICKS = 60;

    /** What a lung finds ({@code AirDensityTable.breathable}, 1 at sea level) at or above which a person breathes unaided. The Overworld curve gives 0.5 at y 192. */
    public static final float UNAIDED_BREATHABLE = 0.5f;
    /** Ticks between the warning cues (laboured breath, swimming head) while the air is too thin and nothing supplies it. */
    public static final int WARNING_INTERVAL = 40;

    /** Cells a cabin's flood fill visits before it gives up and calls the volume open to space. */
    public static final int CABIN_CELL_CAP = 4096;
    /** mB of air a sealed cell of cabin holds. */
    public static final float CABIN_AIR_MB_PER_CELL = 1000f;
    /** mB of cabin air a person breathes each tick. */
    public static final float CABIN_USE_MB_PER_TICK = 5f;
    /** mB of cabin air each open face of the hull loses each tick, against a vacuum. */
    public static final float CABIN_LEAK_MB_PER_HOLE_TICK = 10f;
    /** Ticks between a person's cabin being scanned again for changes in its walls. */
    public static final int CABIN_RESCAN_TICKS = 20;
    /** mB an Air Valve holds. */
    public static final int AIR_VALVE_TANK_MB = 2000;

    /** Half the side, in chunks, of the square of map the Gap shows below the craft (one pixel a chunk). */
    public static final int MAP_RADIUS_CHUNKS = 48;
    /** Ticks between a player's map requests. */
    public static final int MAP_REQUEST_INTERVAL_TICKS = 40;
    /** How far (blocks) below the craft the map plane is drawn. */
    public static final float MAP_DEPTH_BLOCKS = 220f;
    /** Blocks of climb above {@link #ENTRY_Y} that add one more {@link #MAP_DEPTH_BLOCKS} to the plane's depth: higher up the map lies further off, so it shows smaller and coarser. */
    public static final float MAP_CLIMB_PER_DEPTH = 100f;
    /** The least and most the plane's depth is, as shares of {@link #MAP_DEPTH_BLOCKS}. */
    public static final float MAP_DEPTH_MIN_SHARE = 0.5f, MAP_DEPTH_MAX_SHARE = 4f;
}
