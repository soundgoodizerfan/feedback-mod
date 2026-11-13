// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.column;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.fluid.FluidTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.Mb;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.machine.vessel.VesselTuning;

/**
 * Tuning constants for the machine/column code.
 * Every figure is a placeholder or invented unless its comment says it comes from the slice design.
 */
public final class PetrochemTuning {

    private PetrochemTuning() {
    }
    /**
     * Invented. Work drawn out of the boiler's own heat per mB of Steam made. Steam is where the
     * heat goes: without this a boiler at working temperature made steam for free, and output
     * did not depend on the fire at all. With it, steam output is capped by the heat flowing in
     * on the difference -- {@link ThermalTuning#FIRE_CONDUCTANCE} x (fire - boiler) -- so a weak fire holds
     * the boiler at {@link MachineTuning#BOILER_WORKING_TU} making a trickle, and full output needs a fire
     * roughly 330 Tu hotter than that.
     */
    public static final float BOILER_WORK_PER_MB_STEAM = 20f;

    /**
     * Invented. The Refrigeration Unit (food spec 8): amps it asks for (it has no thermostat, the
     * player's breaker is the switch), the ticks an Ice Box keeps trusting it after its last pulse,
     * the temperature its radiator is driven toward while running (it settles about 6/7 of the way
     * from the room, over the scald line), and its mass and leak (warms in about half a minute,
     * cools in a few minutes).
     */
    public static final float REFRIGERATION_AMPS = 2f;

    public static final Tu REFRIGERATION_HOT_TU = new Tu(130f);

    public static final ThermalMass REFRIGERATION_MASS = new ThermalMass(400f);

    public static final Conductance REFRIGERATION_LEAK = new Conductance(0.1f);

    /**
     * Invented. The cold room (roadmap SV4): the unit fills a room out to this many blocks each way,
     * every this many ticks; a wall face leaks its material's thermal conductivity, or the default
     * where its block is no material; an opening (a gap, an open door, sky) leaks far more; each
     * running unit pulls this much Work per tick out of the room; the room drifts a share of the way
     * to its target each second, slower the bigger it is. A unit with a compressor stage (a Condenser
     * against it) reaches the frozen set point, any other the chilled one ({@link MachineTuning#ICEBOX_COLD_TU}).
     */
    public static final int ROOM_RADIUS = 7;

    public static final int ROOM_FILL_TICKS = 100;

    public static final float ROOM_WALL_DEFAULT_LEAK = 2e-4f;

    public static final float ROOM_OPENING_LEAK = 0.01f;

    public static final float ROOM_PULL = 2f;

    public static final float ROOM_DRIFT_PER_SECOND = 0.05f;

    public static final float ROOM_VOLUME_REF = 100f;

    public static final Tu REFRIGERATION_FROZEN_TU = new Tu(-18f);

    /**
     * Invented. A Pressure Vessel's fixed internal volume -- the free-air volume it holds at 1 Pu,
     * so pressure is plain air over this, in the units {@link
     * io.github.soundgoodizerfan.feedback.machine.bellows.Blown#addAir} already takes -- see
     * {@code PressureVesselBlockEntity}. Pressure is air over this, so a bigger vessel (a later,
     * unbuilt upgrade) reads a lower pressure for the same bellows work, same shape as {@code
     * ThermalMass} turning Work into Tu.
     */
    public static final Mb PRESSURE_VESSEL_VOLUME = new Mb(1000f);

    /**
     * Invented. A Pressure Vessel is not perfectly sealed -- a small constant bleed, in the same
     * air units as {@link #PRESSURE_VESSEL_VOLUME}, so a vessel nobody is actively pumping settles
     * back to nothing rather than holding whatever it was last left at forever. Small enough that
     * a bellows worked at any real rate climbs faster than this drains.
     */
    public static final Mb PRESSURE_VESSEL_LEAK_PER_TICK = new Mb(1f);

    /** Invented. A relief valve opens above this share of a shell's danger line. */
    public static final float RELIEF_OPENS_AT = 0.9f;

    /** Invented. Jars a Pressure Vessel holds (food spec 8, Canning). */
    public static final int CANNING_SLOTS = 4;

    /**
     * Invented. Pressure raises the boiling point: the hottest the jars can get is {@link FTuning#POT_BOIL_TU}
     * plus this per Pu. At 6, {@link #CANNING_STERILISE_TU} is reached at under 2.7 Pu, well below
     * {@link FluidTuning#PRESSURE_VESSEL_DANGER_PU}, so the working window is about 2.7 to 5 Pu.
     */
    public static final float CANNING_BOIL_PER_PU = 6f;

    /** Invented. The jars' contents must be at or above this, in Tu, to sterilise: hotter than plain boiling, so only a pressure vessel gets there. */
    public static final Tu CANNING_STERILISE_TU = new Tu(116f);

    /** Invented. Ticks at or above {@link #CANNING_STERILISE_TU} to take a jar from raw to sealed (30 s). */
    public static final float CANNING_STERILE_TICKS = 600f;

    /** Invented. Ticks a sealed jar kept at sterilising heat takes to go from firm to full mush (2 min). */
    public static final float CANNING_MUSH_TICKS = 2400f;


    // --- the steam loop ---------------------------------------------------------------------

    /**
     * Invented. The condenser: tank sizes, the most it condenses a tick (matched to an engine's
     * {@link VesselTuning#STEAM_ENGINE_MB_PER_TICK}), and its body. Every mB condensed puts
     * {@link #BOILER_WORK_PER_MB_STEAM} back into that body and it stops at
     * {@link MachineTuning#BOILER_WORKING_TU}, so on its own leak it settles at a trickle: at ambient 20 it sheds
     * 0.05 x 100 = 5 Work a tick, a quarter of an mB. Taking its heat away is the player's problem.
     */
    public static final int CONDENSER_TANK_MB = 1000;

    public static final int CONDENSER_MB_PER_TICK = VesselTuning.STEAM_ENGINE_MB_PER_TICK;

    public static final ThermalMass CONDENSER_MASS = new ThermalMass(40f);

    public static final Conductance CONDENSER_LEAK = new Conductance(0.05f);

    /**
     * Invented. The pump: mB a tick per RPM, so a water wheel's few RPM trickle and a geared-up
     * run keeps a boiler fed (an engine burns 10 mB a tick, so about 20 RPM), and a flat load.
     */
    public static final float PUMP_MB_PER_RPM = 0.5f;

    public static final Su PUMP_LOAD_SU = new Su(8f);

    /** Placeholder (user ruling, 2026-10-23). Extra Su a pump pays per block it lifts: facing up it lifts one, so a column is a stack of pumps each paying. */
    public static final Su PUMP_LIFT_SU_PER_BLOCK = new Su(6f);

    /** Invented. The derrick: the bit's load, depth one casing reaches (blocks), turns a tick per RPM, drive force, the RPM it is rated for, and force absorbed per RPM beyond it. */
    public static final Su DERRICK_LOAD_SU = new Su(14f);

    public static final int DERRICK_CASING_REACH = 8;

    public static final float DERRICK_PASSES_PER_RPM = 0.02f;

    public static final St DERRICK_DRIVE_ST = new St(20f);

    public static final float DERRICK_RATED_RPM = 60f, DERRICK_OVERSPEED_ST_PER_RPM = 0.5f;

    /** Invented. Fu to bore one block; rock hardness per vanilla destroy time when no material names the block; a bit's wear per St it absorbs, its floor, and its hardness with no material. */
    public static final float DERRICK_FU_PER_BLOCK = 400f, DERRICK_ROCK_HARDNESS_PER_DESTROY_TIME = 6f;

    public static final float DERRICK_BIT_WEAR_PER_ST = 0.0002f, DERRICK_BIT_CONDITION_FLOOR = 0.05f,
            DERRICK_BIT_DEFAULT_HARDNESS = 30f;

    /** The magma head (Nether industry): the Y a drill must reach to meet magma, the fire (Tu) it needs to flow at all and to flow freely, mB of melt per RPM at full heat, a bit's service temperature when its material states none, and bit condition lost per Tu over it per tick (all invented). */
    public static final int MAGMA_LEVEL_Y = 28;

    public static final float MAGMA_MIN_TU = 1600f, MAGMA_FULL_TU = 2200f, MAGMA_MB_PER_RPM = 0.4f,
            MAGMA_BIT_SERVICE_DEFAULT_TU = 1500f, MAGMA_BIT_WEAR_PER_TU = 0.000002f;

    /** Invented. The wellhead's choke: share of flow it passes, and the surface pressure it is rated for (danger line, then certain failure). */
    public static final float WELLHEAD_SMALL_FLOW = 0.2f, WELLHEAD_HALF_FLOW = 0.5f, WELLHEAD_OPEN_FLOW = 1f;

    public static final Pu WELLHEAD_SMALL_DANGER_PU = new Pu(30f), WELLHEAD_SMALL_CRITICAL_PU = new Pu(60f);

    public static final Pu WELLHEAD_HALF_DANGER_PU = new Pu(20f), WELLHEAD_HALF_CRITICAL_PU = new Pu(40f);

    public static final Pu WELLHEAD_OPEN_DANGER_PU = new Pu(12f), WELLHEAD_OPEN_CRITICAL_PU = new Pu(24f);

    /** Invented. Depth a hand-placed wellhead assumes; Pu of head per block of depth per unit density; mB a tick per Pu of surface pressure; share of danger where the warning starts. */
    public static final int WELLHEAD_DEFAULT_DEPTH = 32;

    public static final float WELLHEAD_HEAD_PU_PER_BLOCK_DENSITY = 0.4f, WELLHEAD_MB_PER_PU = 2f, WELLHEAD_WARN_FRACTION = 0.7f;

    /** Invented. Below the floor pressure the lift's yield declines toward (never to) the minimum; mB a tick per RPM of the downhole lift. */
    public static final float WELLHEAD_FLOOR_PU = 5f, WELLHEAD_FLOOR_MIN_YIELD = 0.1f, WELLHEAD_LIFT_MB_PER_RPM = 0.5f;

    /** Invented. Coning: the mB a tick per Pu the reservoir sustains, how fast drawing past it raises the cone and how fast easing off lowers it, the share of a draw lost to gas, the light fraction stripped. */
    public static final float WELLHEAD_SUSTAINABLE_MB_PER_PU = 0.5f, WELLHEAD_CONE_RISE = 0.001f,
            WELLHEAD_CONE_RECOVER = 0.0002f, WELLHEAD_CONE_WASTE = 0.5f, WELLHEAD_CONE_STRIP = 0.8f;

    /** Invented. A blowout: mB vented, explosion power, scatter radius for the pools. */
    public static final int WELLHEAD_BLOWOUT_SPILL_MB = 4000, WELLHEAD_BLOWOUT_REACH = 6;

    public static final float WELLHEAD_BLOWOUT_POWER = 3f;


    // --- the fractionating column (spec 6.4, 6.5): invented ----------------------------------------

    /** Invented. The reboiler: feed it holds (mB), the Work a Tu of it and its charge takes, the share of a cut's light mB that boils off each tick. */
    public static final int REBOILER_FEED_TANK_MB = 4000;

    public static final ThermalMass REBOILER_MASS = new ThermalMass(80f);

    public static final float COLUMN_BOIL_PER_TICK = 0.02f;

    /** Invented. A tray: what it holds (mB), what it passes on to a pipe each tick. */
    public static final int TRAY_DRAW_TANK_MB = 1000, TRAY_DRAW_PUSH_MB = 40;

    /** Invented. The three tray types, sharp and slow to flow against loose and fast: share of vapour held back, mB a tick, relative coking. */
    public static final float TRAY_BUBBLE_CAP_EFFICIENCY = 0.92f, TRAY_SIEVE_EFFICIENCY = 0.8f, TRAY_PACKED_EFFICIENCY = 0.7f;

    public static final int TRAY_BUBBLE_CAP_FLOW = 20, TRAY_SIEVE_FLOW = 40, TRAY_PACKED_FLOW = 60;

    public static final float TRAY_BUBBLE_CAP_FOUL = 1f, TRAY_SIEVE_FOUL = 1.5f, TRAY_PACKED_FOUL = 0.6f;

    /** Invented. A tray hotter than this coats with coke, per tick by this much times how far over (as a share of it); steam through a cold column clears this much per mB. */
    public static final float COLUMN_COKE_TU = 420f, COLUMN_FOUL_PER_TICK = 0.0004f, COLUMN_DECOKE_PER_MB = 0.002f;

    /** Invented. Reflux (spec 6.4): the share of boil-up that still leaves at partial and total reflux, and the share of the gap to a perfect cut each closes. Throughput against sharpness. */
    public static final float REFLUX_PARTIAL_THROUGHPUT = 0.7f, REFLUX_TOTAL_THROUGHPUT = 0.4f;

    public static final float REFLUX_PARTIAL_SHARPEN = 0.5f, REFLUX_TOTAL_SHARPEN = 0.8f;


    // --- gas: the turbine and the flare (spec 8.3, 8.4): invented -----------------------------------

    /** Invented. The turbine: fuel tank (mB), full-spool speed and torque, the speed a starter must turn it to light, the spool share above which it holds itself, spool gained a tick when running (SPOOL_UP_PER_TICK), spool lost a tick when off (COAST_PER_TICK, small, so it coasts a long while), mB burned a tick at full spool (FUEL_MB_PER_TICK). */
    public static final int TURBINE_TANK_MB = 2000;

    public static final Rpm TURBINE_RPM = new Rpm(240f);

    public static final Su TURBINE_SU = new Su(300f);

    public static final float TURBINE_STARTER_RPM = 20f, TURBINE_SELF_SUSTAIN = 0.2f, TURBINE_SPOOL_UP_PER_TICK = 0.002f,
            TURBINE_COAST_PER_TICK = 0.0004f, TURBINE_FUEL_MB_PER_TICK = 2f;

    /** Invented. The flare: what it holds (mB) and burns a tick. */
    public static final int FLARE_TANK_MB = 4000, FLARE_MB_PER_TICK = 20;

    /** Invented. The compressor: its load, mB of gas a tick it moves at full speed per RPM, how much it heats the gas (Tu) per stage, and condition lost a tick running against a full line. */
    public static final Su COMPRESSOR_LOAD_SU = new Su(30f);

    public static final float COMPRESSOR_MB_PER_RPM = 0.4f, COMPRESSOR_HEAT_TU = 60f, COMPRESSOR_SURGE_WEAR_PER_TICK = 0.0005f,
            COMPRESSOR_CONDITION_FLOOR = 0.2f;

    /** Invented. The gas tank: what it holds (mB) and mB of gas per Pu (a full tank sits well over the vessel danger line, so it is a real hazard). */
    public static final int GAS_TANK_MB = 16000;

    public static final float GAS_TANK_MB_PER_PU = 1500f;

    /** Invented. Blade wear a tick per share of the service temperature a fuel's flame runs over it (a hot gas on a soft blade is a one-way misuse), and the floor it never passes. */
    public static final float TURBINE_BLADE_WEAR_PER_TICK = 0.002f, TURBINE_BLADE_CONDITION_FLOOR = 0.2f;

    /** Invented. Flameout: unburnt fuel (mB) a flamed-out burner takes in each tick while fuel keeps coming, the most it holds, and what clears each tick once the supply is off. */
    public static final float TURBINE_FLOOD_PER_TICK = 2f, TURBINE_FLOOD_CAP = 400f, TURBINE_FLOOD_CLEAR_PER_TICK = 0.5f;

    /** Invented. Blade stress (spec 8.4): density times speed squared times this is held against the blade material's yield strength, itself derated as the flame runs over its service temperature. Over it the blades wear, the same one-way misuse as a flame over the service temperature; an overspeed trip on the shaft is what prevents it. */
    public static final float TURBINE_STRESS_PER_DENSITY_RPM2 = 0.0004f;

    /** Invented. An oxygen-poor fuel (blaze powder, blaze rod) with no oxidiser: the share of its flame temperature it reaches, and of its burn time it lasts. */
    /** Invented. A magma-head bit whose service temperature is at least this reaches the rich melt (more nickel and cobalt, and chromium and manganese). */
    public static final float MAGMA_RICH_BIT_SERVICE_TU = 1800f;

    /** Invented. A column's spent bottoms: below this light fraction the reboiler's feed has nothing left to boil and a pipe may drain it. */
    public static final float COLUMN_SPENT_LIGHTS = 0.02f;


    // ---- Creeper pen (roadmap 6.6). Invented; tuned by play. ----

    /** Charge units in a creeper's reserve at the base cap; a charged (lightning-struck) creeper holds this times {@link #CREEPER_CHARGED_CAP}. */
    public static final float CREEPER_CAP = 64f, CREEPER_CHARGED_CAP = 2f;

    /** Ticks for an empty reserve to fill to the base cap while both halves of its feed last: 20 minutes. */
    public static final int CREEPER_FILL_TICKS = 24000;

    /** Feed units (an item's oxygen balance, as a magnitude, times this) of each half one charge unit takes. */
    public static final float CREEPER_FEED_PER_CHARGE = 1.5f;

    /** Feed units of each half one item of a propellant (gunpowder) gives: a ready mix, one charge unit per item. */
    public static final float CREEPER_MIX_FEED = 1.5f;

    /** Most feed (units of each half) a creeper's stomach holds. */
    public static final float CREEPER_STOMACH = 96f;

    /** Ticks between a full reserve and the blast: the swell the player can still shear away. */
    public static final int CREEPER_WARNING_TICKS = 600;

    /** Charge units one creeper charge item stands for; a sac of the base cap cuts into this many items. */
    public static final float CREEPER_CHARGE_PER_ITEM = 4f;

    /** Blast power per square root of charge units, and its cap. Full base cap (64) is power 8, the demolition ceiling. */
    public static final float CREEPER_POWER_PER_SQRT = 1f, CREEPER_POWER_MAX = 12f, CREEPER_POWER_MIN = 0.5f;

    /** Chance a shear sets off a full sac (scaled by the square of how full), and what sneaking, padded hands and careful hands each multiply it by. */
    public static final float CREEPER_SHEAR_SHOCK = 0.5f, CREEPER_SHEAR_SNEAK = 0.5f, CREEPER_SHEAR_PADDED = 0.5f, CREEPER_SHEAR_CAREFUL = 0.5f;

    /** Pen hold: a charge smaller than this (units) is too little to shear. */
    public static final float CREEPER_SHEAR_MIN = 1f;

    /** Blocks around a lightning bolt in which a pen is struck. */
    public static final int CREEPER_LIGHTNING_RADIUS = 3;

    /** Volts a charged creeper's blast discharges into everything inside its blast radius. */
    public static final float CREEPER_DISCHARGE_VOLTS = 400f;
}
