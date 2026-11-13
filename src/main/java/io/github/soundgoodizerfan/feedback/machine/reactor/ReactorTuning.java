// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.reactor;

import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;

/**
 * Tuning constants for the machine/reactor code.
 * Every figure is a placeholder or invented unless its comment says it comes from the slice design.
 */
public final class ReactorTuning {

    private ReactorTuning() {
    }

    // --- the reactor (spec 7.4, 7.5): invented -------------------------------------------------------

    /** Invented. Each tank a reactor holds (mB), the mB of gas per Pu it holds, what it pushes out a tick, the Work a Tu of it takes. */
    public static final int REACTOR_TANK_MB = 2000, REACTOR_PUSH_MB = 40;

    public static final float REACTOR_GAS_MB_PER_PU = 40f;

    public static final ThermalMass REACTOR_MASS = new ThermalMass(80f);

    /** Invented. Bed activity lost per mB of sulfur-weighted crude fed, and per batch made over the row's max Tu. */
    public static final float REACTOR_SULFUR_FOUL_PER_MB = 0.0005f, REACTOR_COKE_FOUL_PER_BATCH = 0.05f;

    /** Invented. Catalyst bed regeneration (spec 7.5): the Tu a bed must hold to burn coke off (an oxidiser flowing) or sweep poison off (a reducing gas flowing); mB of that gas used a tick; activity restored per mB for each; Work per mB the burn gives back. */
    public static final float REACTOR_REGEN_TU = 450f, REACTOR_REGEN_MB_PER_TICK = 4f, REACTOR_COKE_CLEAR_PER_MB = 0.002f,
            REACTOR_POISON_CLEAR_PER_MB = 0.0004f, REACTOR_REGEN_HEAT_PER_MB = 6f;

    /** Invented. A bed held over this sinters, permanently: activity lost a tick per share of it over. Reached by regenerating too hot, readable on the bed's own temperature first. */
    public static final float REACTOR_SINTER_TU = 600f, REACTOR_SINTER_PER_TICK = 0.01f;


    // --- reactor core (fission spec; all invented placeholders) ---------------------------------------------------

    /** Thermal mass of the core, Work per Tu. */
    public static final ThermalMass FISSION_CORE_MASS = new ThermalMass(2000f);

    /** The shell reaches this many blocks from the core sideways, and twice this down; the lattice is one cell inside it. */
    public static final int REACTOR_SHELL_RADIUS = 3;

    /** The least density (mu per mB) a block's material needs to count as a wall cell: steel and lead do, stone does not. */
    public static final float REACTOR_WALL_MIN_DENSITY = 7f;

    /** Work per tick one fuel cell makes at power 1. */
    public static final float REACTOR_CELL_WORK = 3f;

    /** Multiplication a live neighbouring fuel cell and a moderator's {@code moderation} each add to a cell. */
    public static final float REACTOR_FUEL_NEIGHBOUR_GAIN = 0.12f, REACTOR_MODERATOR_GAIN = 0.35f;

    /** What a control bundle takes off: per adjacent fuel cell, and across the whole lattice per cell, at full insertion and strength. */
    public static final float REACTOR_ABSORB_ADJACENT_GAIN = 0.6f, REACTOR_ABSORB_GLOBAL_GAIN = 0.04f;

    /** Mean multiplication at which the chain just holds (power 0) and at which it runs at full; power may overshoot to the ceiling. */
    public static final float REACTOR_K_CRITICAL = 1.0f, REACTOR_K_FULL = 1.8f, REACTOR_POWER_CEILING = 1.5f;

    /** A hotter core is less reactive: per Tu over the onset. */
    public static final float REACTOR_TEMPERATURE_FEEDBACK = 0.0002f, REACTOR_FEEDBACK_ONSET_TU = 400f;

    /** The spring that pulls real power toward the target and the damping that keeps it from ringing: seconds to settle, with overshoot. */
    public static final float REACTOR_SPRING = 0.0004f, REACTOR_DAMP = 0.97f;

    /** Decay heat: a share of the running work, falling with this time constant (ticks). */
    public static final float REACTOR_DECAY_SHARE = 0.06f, REACTOR_DECAY_TAU_TICKS = 36000f;

    /** Burnup per tick at power 1 (about two hours), the rod condition floor, and what breeding adds per unit fertile and burn. */
    public static final float REACTOR_BURN_PER_TICK = 0.0000069f, REACTOR_FUEL_CONDITION_FLOOR = 0.05f, REACTOR_BREED_GAIN = 0.6f;

    /** Fuel damage: condition lost per tick per Tu over {@code REACTOR_FUEL_DAMAGE_TU}, and per unit of power over {@code REACTOR_OVERPOWER}. */
    public static final float REACTOR_FUEL_DAMAGE_TU = 1100f, REACTOR_FUEL_DAMAGE_PER_TU = 0.00001f, REACTOR_OVERPOWER = 1.15f, REACTOR_OVERPOWER_DAMAGE = 0.002f;

    /** A trip block in the shell drops the rods over this temperature and lets them reset this far under it. */
    public static final float REACTOR_TRIP_TU = 850f, REACTOR_TRIP_RESET_TU = 200f;

    /** Vessel pressure: none under the onset, this much per Tu over it; danger, critical and the warning share as for any pressure holder. */
    public static final float REACTOR_PRESSURE_ONSET_TU = 900f, REACTOR_PU_PER_TU = 0.01f, REACTOR_WARN_FRACTION = 0.7f;

    public static final Pu REACTOR_DANGER_PU = new Pu(5f), REACTOR_CRITICAL_PU = new Pu(7f);

    /** Steam blast power of a burst, and the slag it scatters: how many blocks, how far, and each one's dose (Ru) and decay time (ticks). */
    public static final float REACTOR_BURST_POWER = 6f, REACTOR_SLAG_RU = 150f;

    public static final int REACTOR_SLAG_BLOCKS = 18, REACTOR_SLAG_REACH = 4;

    public static final int REACTOR_SLAG_DECAY_TICKS = 480000;

    /** The dose a core at full power gives at point blank (Ru). */
    public static final float REACTOR_RUNNING_RU = 400f;

    /** A control rod moves one position this often (ticks), and a full absorber bundle is this many items. */
    public static final int REACTOR_ROD_STEP_TICKS = 20, REACTOR_ABSORBER_FULL_BUNDLE = 8;

    /** Invented. Capacity of each tank of a coolant port, mB. */
    public static final int REACTOR_PORT_TANK_MB = 4000;

    /** Invented. Work a tick one coolant channel carries off per Tu the core stands over the water's boiling point, and the most one carries. */
    public static final float REACTOR_CHANNEL_CONDUCT = 0.03f, REACTOR_CHANNEL_MAX_WORK = 20f;

    /** Invented. Moderation a flowing coolant channel gives a fuel cell beside it: light water slows neutrons; lose the water and this goes. */
    public static final float REACTOR_COOLANT_MODERATION = 0.15f;

    /** Invented. Multiplication a reflector block gives each fuel cell it touches. */
    public static final float REACTOR_REFLECTOR_GAIN = 0.2f;

    /** Invented. A seated Nether star: multiplication it adds to every live cell, the Tu its temperature feedback starts later, and the idle power and cells' worth of heat it makes with no fuel at all. */
    public static final float REACTOR_STAR_K = 0.25f, REACTOR_STAR_FEEDBACK_BONUS_TU = 300f, REACTOR_STAR_IDLE_POWER = 0.12f, REACTOR_STAR_IDLE_CELLS = 3f;

    /** Invented. A spent rod's decay heat the tick it is discharged (work), and the ticks it takes to fall to a third; a pool's slot count, tank (mB) and thermal mass. */
    public static final float REACTOR_SPENT_HEAT = 0.5f, REACTOR_SPENT_TAU_TICKS = 120000f;

    public static final int REACTOR_POOL_SLOTS = 9, REACTOR_POOL_TANK_MB = 8000;

    public static final ThermalMass REACTOR_POOL_MASS = new ThermalMass(400f);

    /** Invented. Dose a pool's rods give at point blank with no water over them (Ru per rod), the share water leaves, and the Tu at which a dry pool's rods slump into slag. */
    public static final float REACTOR_POOL_DRY_RU = 60f, REACTOR_POOL_WET_SHARE = 0.05f, REACTOR_POOL_MELT_TU = 1200f;

    /** Invented. A fast breeder rod's breeding gain in a hard spectrum, and the share of it left when a moderator or flowing coolant touches the cell. */
    public static final float REACTOR_FAST_BREED_GAIN = 1.6f, REACTOR_FAST_MODERATED_SHARE = 0.15f;

    /** Invented. The longest away-time a reloaded core simulates tick by tick, in ticks (an hour). */
    public static final long REACTOR_OFFLINE_MAX_TICKS = 72000L;
}
