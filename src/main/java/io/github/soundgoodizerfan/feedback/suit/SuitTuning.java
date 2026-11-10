// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.suit;

/**
 * Every figure the powered suit reads. All are invented placeholders, tuned in play. Draws are amps (amp-ticks of battery charge per tick,
 * the unit {@code BatteryStack} counts in) as a share of the servo's drive, so a stronger winding draws harder for harder work. Heat is
 * {@code Tu} on the suit, fed by the waste share of what is drawn and shed to the world.
 */
public final class SuitTuning {

    private SuitTuning() {
    }

    // --- heat -----------------------------------------------------------------------------------
    /** Tu a waste amp-tick adds to the suit. */
    public static final float HEAT_PER_WASTE = 0.25f;
    /** Share of the gap to ambient shed each tick in still air; water, rain and cold multiply it. */
    public static final float COOL_RATE = 0.002f, COOL_WATER = 6f, COOL_RAIN = 2f, COOL_COLD = 1.5f;
    /** The suit cuts its servos out above this and comes back below the resume figure (a hysteresis, so it does not flicker). */
    public static final float LIMIT_TU = 110f, RESUME_TU = 80f;
    /** Ticks between wearing a servo while overheated and still asked to work. */
    public static final int OVERHEAT_WEAR_TICKS = 40;

    // --- draws (share of drive per tick) ----------------------------------------------------------
    public static final float STRENGTH_DRAW = 0.2f, JUMP_IDLE_DRAW = 0.02f, DASH_DRAW = 1.0f;
    /** Flat draws in amps for the helmet's sensors. */
    public static final float NIGHT_AMPS = 0.3f, THERMAL_AMPS = 0.8f;
    /** Amp-ticks a point of damage the plating absorbs costs. */
    public static final float PLATING_PER_DAMAGE = 60f;
    /** Heat a point of absorbed damage leaves as waste amp-ticks. */
    public static final float PLATING_WASTE = 6f;
    /** Amp-ticks a jump from the suit costs, flat. */
    public static final float JUMP_DRAW = 12f;

    // --- effects --------------------------------------------------------------------------------
    /** Strength: attack damage and break speed per amp of drive, the most each can give, and the carry the pack gains per amp. */
    public static final float STRENGTH_DAMAGE_PER_AMP = 0.15f, STRENGTH_DAMAGE_MAX = 6f;
    public static final float STRENGTH_BREAK_PER_AMP = 0.05f, STRENGTH_BREAK_MAX = 1f;
    public static final float STRENGTH_CARRY_PER_AMP = 0.1f, STRENGTH_CARRY_MAX = 2f;
    /** Jump: jump strength and safe fall per amp of leg and boot drive, and the most. */
    public static final float JUMP_PER_AMP = 0.03f, JUMP_MAX = 0.3f, FALL_PER_AMP = 0.5f, FALL_MAX = 8f;
    /** Plating: armour per amp of drive and its most; toughness fixed. */
    public static final float PLATING_ARMOUR_PER_AMP = 0.4f, PLATING_ARMOUR_MAX = 8f, PLATING_TOUGHNESS = 2f;
    /** Dash thrust: acceleration (blocks per tick squared) at the reference drive, the drive that is the reference, top speed, the hop on lift-off. */
    public static final float DASH_ACCEL = 0.06f, DASH_REF_AMPS = 22f, DASH_TOP_SPEED = 0.9f, DASH_HOP = 0.25f;
    /** Air density that gives full thrust; thinner air gives less in proportion. */
    public static final float REF_AIR_DENSITY = 1.2f;
    /** Walls: a dash that stops against one at more than this speed wears the legs by one point. */
    public static final float DASH_WALL_SPEED = 0.5f;

    // --- sensors --------------------------------------------------------------------------------
    /** Thermal reach in blocks: base plus per cell of fibre, and the scan interval in ticks. */
    public static final float THERMAL_BASE_RANGE = 12f, THERMAL_RANGE_PER_CELL = 6f, THERMAL_MAX_RANGE = 24f;
    public static final int THERMAL_SCAN_TICKS = 5, THERMAL_MAX_ENTRIES = 256;

    // --- module abilities ---------------------------------------------------------------------------
    /** Cloak: amps a tick to hold the field, and ticks each refresh of the hidden invisibility lasts. */
    public static final float CLOAK_AMPS = 1.2f;
    public static final int CLOAK_REFRESH = 10, CLOAK_TICKS = 40;
    /** Jet: amps a tick of thrust, vertical acceleration (blocks per tick squared) at full power, the climb it tops out at, and the hold it gives against a fall. */
    public static final float JET_AMPS = 2.5f, JET_ACCEL = 0.08f, JET_TOP = 0.45f;
    /** Pulses: amp-ticks each costs, ticks before it can fire again, and its reach in blocks at full power. */
    public static final float SHOCK_COST = 160f, SCAN_COST = 90f, EMP_COST = 220f;
    public static final int SHOCK_COOLDOWN = 100, SCAN_COOLDOWN = 60, EMP_COOLDOWN = 200;
    public static final float SHOCK_RADIUS = 4f, SHOCK_DAMAGE = 5f, SHOCK_PUSH = 1.4f, SCAN_RADIUS = 28f, EMP_RADIUS = 7f;
    /** Ticks a scanned thing glows and a jammed mob stays slowed and blind. */
    public static final int SCAN_GLOW_TICKS = 160, EMP_TICKS = 140;

    // --- sync -----------------------------------------------------------------------------------
    /** Ticks between writing the battery's drain and the suit's state back to the stacks (every change would resend the slot). */
    public static final int WRITE_TICKS = 20;
}
