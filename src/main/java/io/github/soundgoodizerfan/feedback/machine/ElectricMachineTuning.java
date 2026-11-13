// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.unit.Qu;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.TuRate;

/**
 * Tuning constants for the machine code.
 * Every figure is a placeholder or invented unless its comment says it comes from the slice design.
 */
public final class ElectricMachineTuning {

    private ElectricMachineTuning() {
    }
    /**
     * Placeholder. The Rotation Adapter ({@code docs/dev/vehicle.md}, micro rotation): a
     * small housing of bevels from one full-size shaft to four quarter ones, so a gearbox's figures.
     */
    public static final Drag ROTATION_ADAPTER_DRAG_SU_PER_RPM = new Drag(0.2f);

    public static final Inertia ROTATION_ADAPTER_INERTIA = new Inertia(4f);

    /**
     * Placeholder. Amps a wire carries per unit of its material's {@code ampacity} (copper = 1),
     * so copper carries 4 A. Resistance is not modelled; ampacity is the whole of "how much".
     */
    public static final float WIRE_AMPS_PER_AMPACITY = 4f;

    /**
     * Placeholder. The CABLE_GAUGE table: cross-section area of each cable gauge against the
     * standard one (user ruling 2026-11-03, four times the copper per step). A cable's rating is
     * its material's base rating times this; its volts lost per amp per block is the material's
     * {@code drop} divided by it. Copper: thin 1 A and 0.2 V, standard 4 A and 0.05, heavy 16 A and
     * 0.0125, bus bar 64 A and 0.003125. A breaker of a gauge trips at {@link #BREAKER_TRIP_AMPS}
     * times this, and its contacts lose {@link #BREAKER_DROP_PER_AMP} divided by it.
     */
    public static final float CABLE_GAUGE_THIN_AREA = 0.25f, CABLE_GAUGE_STANDARD_AREA = 1f,
            CABLE_GAUGE_HEAVY_AREA = 4f, CABLE_GAUGE_BUS_AREA = 16f;

    /**
     * Placeholder. Cables one craft yields, per gauge: three fine wires in a row, three wires in a
     * row, a 2x2 of wires, one plate. Each cable costs its area in wires, so the yield is the
     * recipe's copper over the gauge's area.
     */
    public static final int CABLE_GAUGE_THIN_YIELD = 3, CABLE_GAUGE_STANDARD_YIELD = 3,
            CABLE_GAUGE_HEAVY_YIELD = 1, CABLE_GAUGE_BUS_YIELD = 1;

    /** Placeholder. A wire block's thermal mass and leak. Mass over leak is 40 ticks. */
    public static final ThermalMass WIRE_MASS = new ThermalMass(2f);

    public static final Conductance WIRE_LEAK = new Conductance(0.05f);

    /**
     * Placeholder. Work per tick put into a wire for every amp over its rating. Nothing is put in
     * at or below the rating, so a wire inside its rating never warms and never wears from uptime.
     * At 2.5 against {@link #WIRE_LEAK}, copper at 6 A (2 A over) settles 100 Tu over ambient.
     */
    public static final float WIRE_OVERLOAD_WORK_PER_AMP = 2.5f;

    /** Placeholder. Above this the insulation layer burns off, before the conductor is touched (6.4 stage 1). */
    public static final Tu WIRE_INSULATION_LIMIT_TU = new Tu(100f);

    /** Placeholder. Ticks between sparks (and their crackle) on an arc that is carrying. */
    public static final int ARC_SPARK_INTERVAL_TICKS = 20;

    /**
     * Placeholder. Conductor condition lost per tick at a temperature twice its material's
     * service temperature, scaled linearly by how far past service it is (6.4 stage 2): zero at
     * service, this much at 2x. 0.002 means a conductor held at 2x service is gone in 500 ticks.
     */
    public static final float WIRE_DEGRADE_PER_TICK = 0.002f;

    /** Invented. A quenched superconducting cable heats by this Work per amp a tick, and recovers once it is back within this many Tu of ambient and carrying no more than its rating. */
    public static final float WIRE_QUENCH_WORK_PER_AMP = 4f, WIRE_QUENCH_RECOVER_MARGIN_TU = 5f;

    /** Placeholder. Amps a galvanic cell holds its voltage up to: a curiosity, not a supply. */
    public static final float GALVANIC_CELL_AMPS = 1f;

    /**
     * Placeholder (user ruling, 2026-10-23: a cell eats its anode). Amp-ticks a full anode lasts: 24000 is 20 minutes
     * at the cell's full {@link #GALVANIC_CELL_AMPS}. An ingot of the anode metal fed to the cell restores
     * {@link #GALVANIC_ANODE_PER_INGOT} of a full anode.
     */
    public static final float GALVANIC_ANODE_AMP_TICKS = 24000f, GALVANIC_ANODE_PER_INGOT = 0.5f;

    /**
     * Placeholder. An electrostatic machine's pulse: volts, amps and how many ticks one turn of
     * the handle lasts. HV (ruled 2026-10-23): high volts, tiny amps.
     */
    public static final float ELECTROSTATIC_VOLTS = ElectricTuning.ELECTRIC_HV_VOLTS;

    public static final float ELECTROSTATIC_AMPS = 0.5f;

    public static final int ELECTROSTATIC_PULSE_TICKS = 20;

    /**
     * Placeholder. The electromagnet. It wants one cell's worth of current, rated
     * LV. Condition falls by {@code WEAR_PER_EXCESS} per tick for every whole rating of voltage it
     * is over (so 3 over loses 0.0015 a tick), to a floor above zero; the same excess puts
     * {@code HEAT_PER_EXCESS} Work a tick into its body. **[OPEN]** the scale (6.5).
     */
    public static final float ELECTROMAGNET_AMPS = 1f;

    public static final float ELECTROMAGNET_CONDITION_FLOOR = 0.5f;

    public static final float ELECTROMAGNET_WEAR_PER_EXCESS = 0.0005f;

    public static final float ELECTROMAGNET_HEAT_PER_EXCESS = 1f;

    public static final ThermalMass ELECTROMAGNET_MASS = new ThermalMass(8f);

    public static final Conductance ELECTROMAGNET_LEAK = new Conductance(0.04f);

    /** Placeholder. Pull range in blocks at full field, per-pull velocity change, and ticks between pulls. */
    public static final float ELECTROMAGNET_RANGE = 4f;

    /** Invented. Field strength (0 to 1) an Electromagnet needs to magnetise a sintered magnet blank: full rated volts and amps. */
    public static final float MAGNETISE_FIELD = 0.95f;

    public static final float ELECTROMAGNET_PULL = 0.08f;

    public static final int ELECTROMAGNET_PULL_INTERVAL_TICKS = 2;

    /**
     * Placeholders. The glowstone lamp (E3): an electroluminescent panel that wants {@code RATED_AMPS} at
     * {@code RATED_VOLTS} (MV, 6 V, 0.5 A, about 3 W). Its brightness step comes from the sink's rate
     * fraction ({@code ElectricRules.rate}) times condition: {@code STEP_THRESHOLDS} are the fractions at
     * which it goes dim, bright and full, and {@code HYSTERESIS} is the gap each way around each so a line
     * near a boundary never flickers. {@code FULL_LIGHT} is the block light of the full step (dim and bright
     * are a third and two thirds of it); the phosphor material's {@code luminance} is the same figure for a
     * later phosphor. Volts at or above {@code BURNOUT_VOLT_FACTOR} times rated burn the panel out at once
     * (a cheap part, the digest's accepted exception); below that, each whole rating of excess volts costs
     * {@code CONDITION_LOSS_PER_EXCESS} condition a tick, to {@code CONDITION_FLOOR}.
     */
    public static final float LAMP_RATED_VOLTS = ElectricTuning.ELECTRIC_MV_VOLTS;

    public static final float LAMP_RATED_AMPS = 0.5f;

    public static final float[] LAMP_STEP_THRESHOLDS = {0.05f, 0.35f, 0.75f};

    public static final float LAMP_HYSTERESIS = 0.04f;

    public static final float LAMP_BURNOUT_VOLT_FACTOR = 2f;

    public static final float LAMP_CONDITION_LOSS_PER_EXCESS = 0.001f;

    public static final float LAMP_CONDITION_FLOOR = 0.3f;

    public static final int LAMP_FULL_LIGHT = 15;

    /**
     * Placeholder. The electrolytic cell. Current is the rate, so this is the one
     * figure that turns amps into metal: Qu moved per amp per tick (Faraday's constant, in this
     * mod's units; no type, as {@code Work} has none). At {@link #ELECTROLYTIC_CELL_AMPS} an ingot
     * takes 600 t (30 s); a galvanic cell's single amp takes 4800 t (4 min). Was 1/480 when an
     * ingot was 10 Qu; scaled by 9/10 for the nugget definition so those times stand.
     */
    public static final float QU_PER_AMP_TICK = 0.001875f;

    public static final float ELECTROLYTIC_CELL_AMPS = 8f;

    /**
     * Placeholder. Volts a cathode needs over and above the gap in electrode potential: the noblest
     * metal in solution plates at this, a less noble one at this plus the gap (zinc behind copper:
     * 0.1 + 1.1 = 1.2 V). Real overpotential, rounded.
     */
    public static final float ELECTROLYSIS_OVERPOTENTIAL_VOLTS = 0.1f;

    /**
     * Water's decomposition potential, real (1.23 V). With no anode left to dissolve, the only
     * thing the anode can do is split water, so below this an empty cell carries nothing.
     */
    public static final float ELECTROLYSIS_WATER_VOLTS = 1.23f;

    /** Placeholder. The cell's water tank, and the least water that still conducts. */
    public static final int ELECTROLYTIC_CELL_TANK_MB = 1000;

    public static final int ELECTROLYTIC_CELL_MIN_WATER_MB = 100;

    /**
     * Placeholder. The share of the dominant metal a cathode needs to come off as ingots; below
     * it the whole deposit is that metal's scrap. Spoil only for now.
     */
    public static final float ELECTROLYTIC_PURE_FRACTION = 0.95f;

    /** Placeholder. Over-voltage cracks the crystal: condition lost per volt over the cell's rating, per tick. */
    public static final float MASS_PROBE_WEAR_PER_EXCESS = 0.002f;

    /**
     * Placeholder. The most current an ingot-sized piece takes before its coat burns; a piece's
     * surface goes as its size to the two-thirds, so a block takes about four times this and a
     * nugget about a quarter. A cell or a fine dynamo plates an ingot clean, a coarse dynamo burns
     * it and plates a block clean. Over {@link #PLATING_BURNT_SHARE} of the coat grown burnt and
     * the whole coat is.
     */
    public static final float PLATING_BURN_AMPS_PER_INGOT = 2.5f;

    public static final float PLATING_BURNT_SHARE = 0.1f;

    /**
     * Placeholder. The heating coil. The current it asks for, and Joule's law in
     * this mod's units: Work per volt-amp per tick (no type, as Faraday's {@link #QU_PER_AMP_TICK}
     * has none). Chosen so a fine dynamo at a steam engine's speed (6 V at 2 A) holds a small
     * vessel near 290 Tu -- just under a copper element's limit -- and a hand-cranked one near
     * 560 Tu, past copper's and iron's both. Not yet a smelter: that is power a later source owes.
     */
    public static final float COIL_AMPS = 2f;

    public static final float COIL_WORK_PER_WATT_TICK = 1.5f;

    /** Placeholder. An element is a little wire: small mass, so it follows its power fast. */
    public static final ThermalMass COIL_MASS = new ThermalMass(5f);

    public static final Conductance COIL_LEAK = new Conductance(0.04f);

    /** Placeholder. Condition lost per tick per Tu the element runs over its metal's service temperature. */
    public static final float COIL_WEAR_PER_TU_EXCESS = 0.000005f;

    public static final float COIL_CONDITION_FLOOR = 0.5f;

    /**
     * Placeholder. The breaker: it trips above a copper cable's rating, the thing
     * it protects; each opening under load costs condition per amp broken (the arc); and its
     * contacts lose this many volts per amp, divided by condition, so a pitted breaker sags the
     * line. Never broken: it keeps switching at its floor.
     */
    public static final float BREAKER_TRIP_AMPS = WIRE_AMPS_PER_AMPACITY;

    public static final float BREAKER_ARC_WEAR_PER_AMP = 0.01f;

    public static final float BREAKER_CONDITION_FLOOR = 0.25f;

    public static final float BREAKER_DROP_PER_AMP = 0.02f;

    /**
     * Placeholder. A fuse's fine wire carries its metal's ampacity times this before it warms
     * (tin 2.4 A, lead 1.3 A, copper 16 A), and is given this much Work per amp over -- ten times a
     * cable's, a thin wire -- so tin a little over its rating melts in about a second.
     */
    public static final float FUSE_AMPS_PER_AMPACITY = 16f;

    public static final float FUSE_OVERLOAD_WORK_PER_AMP = 25f;

    /** Placeholder. Over-voltage on the cell, the electromagnet's shape. **[OPEN]** the scale (6.5). */
    public static final float ELECTROLYTIC_CELL_CONDITION_FLOOR = 0.5f;

    public static final float ELECTROLYTIC_CELL_WEAR_PER_EXCESS = 0.0005f;

    public static final float ELECTROLYTIC_CELL_HEAT_PER_EXCESS = 1f;

    public static final ThermalMass ELECTROLYTIC_CELL_MASS = new ThermalMass(40f);

    public static final Conductance ELECTROLYTIC_CELL_LEAK = new Conductance(0.04f);

    /**
     * Placeholder. Misuse (an over-rated draw, or volts past the rated band), the electromagnet's rule: Work per tick put
     * into the body as a share of the throughput per unit over, condition lost per unit over per tick, and the floor
     * condition falls to. Heat past the housing's service temperature costs condition per Tu over per tick.
     */
    public static final float MACHINE_HEAT_PER_EXCESS = 0.05f, MACHINE_WEAR_PER_EXCESS = 0.0005f,
            MACHINE_CONDITION_FLOOR = 0.5f, MACHINE_HEAT_WEAR_PER_TU = 0.00002f;

    /**
     * Placeholder. The water jacket: tank size, the multiple of the housing's shed rate it gives while it holds water and
     * the body is above ambient, and the ticks one millibucket of that water lasts.
     */
    public static final int JACKET_TANK_MB = 2000;

    public static final float JACKET_SHED_MULTIPLE = 2f;

    public static final int JACKET_TICKS_PER_MB = 20;

    /**
     * Placeholder. The Leyden jar. Capacitance in amp-ticks per volt: one
     * electrostatic pulse (1 A for 20 ticks) puts 20 amp-ticks in, which fills it to its source's
     * 1.5 V with a little to spare. It charges at a cell's current, so a generator or cell is not
     * overdrawn by filling it, and gives back up to {@code BURST_AMPS}. It loses
     * {@code LEAK_PER_TICK} of its charge a tick (half in about 17 s). {@code SWITCH_VOLTS} is the
     * margin it flips role on, so a jar sitting at its supply's voltage does not chatter.
     */
    public static final float LEYDEN_JAR_CAPACITANCE = 10f;

    public static final float LEYDEN_JAR_CHARGE_AMPS = 1f;

    public static final float LEYDEN_JAR_BURST_AMPS = 8f;

    public static final float LEYDEN_JAR_LEAK_PER_TICK = 0.002f;

    public static final float LEYDEN_JAR_SWITCH_VOLTS = 0.01f;

    public static final float BATTERY_DISCHARGE_TICKS = 6000f, BATTERY_CHARGE_TICKS = 3000f;

    public static final float BATTERY_SWITCH_VOLTS = 0.05f;

    /** Volts per cell over the terminal at which a battery asks its full charge current; under it the ask tapers to nothing, so a weak line settles instead of sagging under the terminal. */
    public static final float BATTERY_TAPER_VOLTS_PER_CELL = 0.15f;

    /** Share of the gap to its target a battery's charge ask closes each tick; small enough that a long cable's sag cannot make it ring. */
    public static final float BATTERY_ASK_SMOOTHING = 0.1f;

    public static final float BATTERY_SELF_DISCHARGE_PER_DAY = 0.01f;

    public static final float BATTERY_GASSING_FACTOR = 1.1f, BATTERY_TRICKLE_SHARE = 0.05f;

    /** mB of water electrolysed and of hydrogen released per amp-tick that goes into a full cell. */
    public static final float BATTERY_VENT_WATER_PER_AMP_TICK = 0.5f, BATTERY_VENT_GAS_PER_AMP_TICK = 0.5f;

    /** Water below this share of the tank exposes plates: capacity falls linearly to {@code BATTERY_DRY_CAPACITY} when empty. */
    public static final float BATTERY_WATER_LOW_SHARE = 0.2f, BATTERY_DRY_CAPACITY = 0.4f;

    /** Hydrogen mB at which the weep shows, at which a flame next door sets it off, and the blast power per mB of it (capped). */
    public static final float BATTERY_GAS_WEEP_MB = 5f, BATTERY_GAS_BLAST_MB = 40f, BATTERY_GAS_BLAST_PER_MB = 0.02f, BATTERY_GAS_BLAST_MAX = 3f;

    /** Share of the loose gas that drifts off per second when no holder takes it. */
    public static final float BATTERY_GAS_LEAK_PER_SECOND = 0.1f;

    /** Overdraw: condition lost and case Work per share of the discharge limit drawn over it, per tick. */
    public static final float BATTERY_OVERDRAW_WEAR = 0.0001f, BATTERY_OVERDRAW_HEAT = 6f, BATTERY_CONDITION_FLOOR = 0.1f;

    /** Case heat: above this Tu the plates lose condition each tick; and the case's thermal mass and leak per cell (a charging cell settles about 7 Tu over ambient, so correct use never reaches the hot line). */
    public static final float BATTERY_HOT_TU = 50f, BATTERY_HEAT_WEAR_PER_TU = 0.00002f;

    public static final float BATTERY_MASS_PER_CELL = 40f, BATTERY_LEAK_PER_CELL = 0.2f;

    /** Overvoltage past the case's insulation band: condition lost per share over, per tick. */
    public static final float BATTERY_OVERVOLT_WEAR = 0.00005f;

    /** Body temperature at which cold capacity bottoms out at the material's cold factor (full at ambient). */
    public static final float BATTERY_DEEP_COLD_TU = 0f;

    /**
     * Placeholders, the Rectifier (roadmap E4). Set against a battery and powered, it lets that battery charge in
     * {@code RECTIFIER_CHARGE_TICKS} (capacity over that, about a minute empty to full) instead of
     * {@code BATTERY_CHARGE_TICKS}; the extra current is the battery's own heat and gassing logic's to answer. It asks a small
     * standing {@code RECTIFIER_AMPS} at MV whether or not a battery is there. A battery re-reads its neighbours every
     * {@code RECTIFIER_SCAN_TICKS} ticks, never every tick.
     */
    public static final float RECTIFIER_AMPS = 0.25f, RECTIFIER_CHARGE_TICKS = 1200f;

    public static final int RECTIFIER_SCAN_TICKS = 20;

    /** Invented. Ticks between kill cycles. */
    public static final int DARKCELL_CYCLE_TICKS = 40;

    /** Invented. Ticks a cell takes to spawn one mob per 100 blocks of volume. */
    public static final float DARKCELL_SPAWN_TICKS_PER_100_BLOCKS = 200f;

    /** Invented. Mu of mob a cell holds per block of interior volume. */
    public static final float DARKCELL_MASS_PER_BLOCK = 0.6f;

    /** Invented. Most interior blocks a cell may scan. */
    public static final int DARKCELL_MAX_VOLUME = 2500;

    /** Invented. Share of the population a lethal method works on each cycle. */
    public static final float DARKCELL_THROUGHPUT = 0.5f;

    /** Invented. Shield hit points per point of wall hardness. */
    public static final float DARKCELL_SHIELD_PER_HARDNESS = 40f;

    /** Invented. Shield damage per second per point of mob attack, at full pressure. */
    public static final float DARKCELL_ATTACK_K = 0.05f;

    /** Invented. Shield a wall block's hardness restores when a repairer is spent, per hardness. */
    public static final float DARKCELL_REPAIR_PER_HARDNESS = 20f;

    /** Invented. Mobs a breach lets out at most, and the interior blocks that make room for one. */
    public static final int BREACH_MAX_MOBS = 40;

    public static final float BREACH_BLOCKS_PER_MOB = 4f;

    /** Invented. The share of a kill's XP a Catalyst Tap keeps from a machine, before the method's own share. */
    public static final float MACHINE_XP_SHARE = 0.5f;

    /** Invented. Items one cell's kill cycle may add to its output. */
    public static final int DARKCELL_MAX_DROPS_PER_CYCLE = 64;


    // --- pivot mast (docs/specs/feedback_automation_spec.md, section 2). Every figure here is invented. ---

    /** Invented. The arm turns at this share of the shaft's phase: a slow, heavy sweep. */
    public static final float PIVOT_GEAR = 0.25f;

    /** Invented. The mast's own draw, Su. */
    public static final float PIVOT_BASE_SU = 4f;

    /** Invented. Draw per block of arm, Su: the lever. */
    public static final float PIVOT_SU_PER_BLOCK = 1.5f;

    /** Invented. Extra draw per block of arm while a head hangs on it, Su. */
    public static final float PIVOT_HEAD_SU_PER_BLOCK = 0.8f;

    /** Invented. Most cells one tick may work, so a fast arm cannot spike the server. */
    public static final int PIVOT_CELLS_PER_TICK = 24;

    /** Invented. St a head needs per point of block hardness to break it. */
    public static final float PIVOT_BREAK_ST_PER_HARDNESS = 2f;

    /** Invented. Damage a striking tip does per St of its head's ceiling. */
    public static final float PIVOT_STRIKE_DAMAGE_PER_ST = 0.1f;

    /** Invented. Hardest a single strike may hit. */
    public static final float PIVOT_STRIKE_MAX_DAMAGE = 12f;

    /** Invented. Head condition lost per St absorbed. */
    public static final float PIVOT_HEAD_WEAR_PER_ST = 0.0005f;

    /** Invented. A pivot head never wears below this condition. */
    public static final float PIVOT_HEAD_FLOOR = 0.15f;

    /** Invented. Volts at or above which a bare touch is a shock to be reckoned with: MV. */
    public static final float SHOCK_MIN_VOLTS = ElectricTuning.ELECTRIC_MV_VOLTS;

    /**
     * Invented, grid spec 4.1. Arc flash: a Breaker opening under load at {@link #BREAKER_FLASH_MIN_BAND_VOLTS} or more burns whatever
     * unprotected stands within {@link #BREAKER_FLASH_RADIUS} blocks, for {@link #BREAKER_FLASH_DAMAGE_PER_AMP} per amp broken (capped) and
     * {@link #BREAKER_FLASH_FIRE_TICKS} ticks of fire. Past {@link #BREAKER_FLASH_WARN_LOAD} of its trip current, the same breaker hums and
     * sparks every {@link #BREAKER_FLASH_WARN_TICKS} ticks, so a correct setup is never a surprise.
     */
    public static final float BREAKER_FLASH_MIN_BAND_VOLTS = ElectricTuning.ELECTRIC_EHV_VOLTS * 0.9f, BREAKER_FLASH_RADIUS = 3f,
            BREAKER_FLASH_DAMAGE_PER_AMP = 0.25f, BREAKER_FLASH_DAMAGE_CAP = 30f, BREAKER_FLASH_WARN_LOAD = 0.8f;

    public static final int BREAKER_FLASH_FIRE_TICKS = 100, BREAKER_FLASH_WARN_TICKS = 20;

    /**
     * Invented, grid spec 5. Solid-state DC converter: {@code CONVERTER_EFFICIENCY} of the power in comes out (about 95 percent);
     * {@code CONVERTER_RATED_WATTS} (volts times amps) is the most it passes; the loss becomes heat in the block at
     * {@code CONVERTER_HEAT_SCALE} work per unit lost, shed at {@code CONVERTER_LEAK}, multiplied by
     * {@code CONVERTER_HEATSINK_LEAK_MULTIPLE} for each adjacent heatsink (at most {@code CONVERTER_MAX_HEATSINKS}); past
     * {@code CONVERTER_SERVICE_TU} it loses condition down to {@code CONVERTER_CONDITION_FLOOR}.
     */
    public static final float CONVERTER_EFFICIENCY = 0.95f, CONVERTER_RATED_WATTS = 3840f, CONVERTER_STANDING_AMPS = 0.02f,
            CONVERTER_HEAT_SCALE = 0.04f, CONVERTER_HEATSINK_LEAK_MULTIPLE = 2f, CONVERTER_SERVICE_TU = 250f,
            CONVERTER_CONDITION_FLOOR = 0.4f;

    public static final int CONVERTER_MAX_HEATSINKS = 4, CONVERTER_SCAN_TICKS = 20;

    public static final ThermalMass CONVERTER_MASS = new ThermalMass(10f);

    public static final Conductance CONVERTER_LEAK = new Conductance(0.02f);


    // --- the electrified fence --------------------------------

    /** Invented. Ticks before the same entity can be shocked by a fence again. */
    public static final int FENCE_SHOCK_COOLDOWN_TICKS = 10;

    /** Invented. Amps one shock pulls from its run for the tick. */
    public static final float FENCE_HIT_AMPS = 1f;

    /** Placeholder. Coherer: amps its welded tube carries before sintering; condition lost per tick per amp over; floor (a tap then frees it with probability condition). */
    public static final float COHERER_RATING_AMPS = 2f;

    public static final float COHERER_SINTER_PER_AMP = 0.002f;

    public static final float COHERER_CONDITION_FLOOR = 0.2f;

    /** Placeholder. Cat's whisker: spots on a crystal; hold ticks after a heard spark; spots below this sensitivity hear nothing; amps that fuse the spot under the whisker. */
    public static final int WHISKER_SPOTS = 8;

    public static final int WHISKER_HOLD_TICKS = 10;

    public static final float WHISKER_MIN_SENSITIVITY = 0.05f;

    public static final float WHISKER_FUSE_AMPS = 0.5f;

    /** Placeholder. Relay: electromagnet field that pulls in / lets go; contact rating; arc wear per amp broken (as the breaker's); overload wear per amp per tick over rating; floor. */
    public static final float RELAY_PULL_IN_FIELD = 0.6f;

    public static final float RELAY_DROP_OUT_FIELD = 0.3f;

    public static final float RELAY_CONTACT_AMPS = WIRE_AMPS_PER_AMPACITY;

    public static final float RELAY_ARC_WEAR_PER_AMP = 0.01f;

    public static final float RELAY_OVERLOAD_WEAR_PER_AMP = 0.002f;

    public static final float RELAY_CONDITION_FLOOR = 0.2f;

    public static final float ENVELOPE_ABSOLUTE_OFFSET = 273f;

    public static final float ENVELOPE_PU_PER_ATMOSPHERE = 1f;

    public static final ThermalMass ENVELOPE_MASS = new ThermalMass(20f);

    public static final Conductance ENVELOPE_LEAK = new Conductance(0.05f);

    /** Placeholder. A sealed envelope reheated past its seal: gauge Pu where it starts to let go, and where it certainly does. */
    public static final Pu ENVELOPE_DANGER_PU = new Pu(0.1f);

    public static final Pu ENVELOPE_CRITICAL_PU = new Pu(0.3f);

    /** Placeholder. The screen glows only at or below this gauge pressure (negative is vacuum). */
    public static final float SCREEN_MIN_VACUUM_PU = -0.3f;

    /** Placeholder. Burn-in: burn added per tick held at full glow, and burn at which the next brightest level is lost (three levels: dead at 1). */
    public static final float SCREEN_BURN_PER_TICK = 0.0001f;

    /** Placeholder. Oscilloscope: ticks between samples, samples kept, amps at the top of the screen (above it the reading pegs), finest step. */
    public static final int SCOPE_SAMPLE_TICKS = 2;

    public static final int SCOPE_HISTORY = 16;

    public static final float SCOPE_FULL_SCALE_AMPS = 4f;

    public static final float SCOPE_RESOLUTION_AMPS = 0.25f;

    /** Placeholder. Over its trace rating an element fuses with this chance per tick. */
    public static final float CIRCUIT_FUSE_CHANCE = 0.1f;

    /** Placeholder. Whisker stage, one constant each in order: extra reach per circuit element (STAGE_GAIN), share of the hold a full-speed circuit cuts (STAGE_SNAP), base spread of the hunt band (BAND_SPREAD). */
    public static final float WHISKER_STAGE_GAIN = 0.15f;

    public static final float WHISKER_STAGE_SNAP = 0.5f;

    public static final float WHISKER_BAND_SPREAD = 0.2f;

    /** Invented. Air a culture holds (mB) and breathes a tick; the dish pulls it from a neighbouring gas holder once a second. */
    public static final int CULTURE_AIR_BUFFER_MB = 200;

    public static final float CULTURE_AIR_MB_PER_TICK = 0.1f;

    /** Invented. The electrolyser: amps it asks for, mB of feed split per tick per amp at full condition (8 A splits 0.4 mB a tick), its feed and electrode tanks (mB), and what over-voltage costs it. */
    public static final float ELECTROLYSER_AMPS = 8f, ELECTROLYSER_MB_PER_AMP_TICK = 0.05f,
            ELECTROLYSER_CONDITION_FLOOR = 0.2f, ELECTROLYSER_WEAR_PER_EXCESS = 0.00005f;

    public static final int ELECTROLYSER_FEED_MB = 2000, ELECTROLYSER_GAS_MB = 2000;

    /** Invented. The electrolyser as a hot body (a molten bath): thermal mass, leak (bath holds near 1000 Tu at 4 V and 8 A), the mB of bath a solvent row needs to run, and what an anode effect (a bath row with no solute left) costs a tick: condition and mB of bath. */
    public static final ThermalMass ELECTROLYSER_MASS = new ThermalMass(80f);

    public static final Conductance ELECTROLYSER_LEAK = new Conductance(0.03f);

    public static final int ELECTROLYSER_SOLVENT_MIN_MB = 500;

    public static final float ELECTROLYSER_ANODE_EFFECT_WEAR = 0.0001f, ELECTROLYSER_ANODE_EFFECT_BATH_MB = 0.05f;

    /** Invented. mB of a toxic gas that seeps from a full electrode tank with nowhere to go, once a second. */
    public static final int ELECTROLYSER_SEEP_MB = 50;

    /** Invented. A ripe harvest yields this many sculk blocks and veins, and a well-fed one buds a spare seed with this chance. */
    public static final int CULTURE_HARVEST_BLOCKS = 2, CULTURE_HARVEST_VEINS = 2, CULTURE_HARVEST_SHARDS = 1;

    public static final float CULTURE_SEED_CHANCE = 1f / 3f;

    /** Invented. Blocks around a Catalyst Tap within which a death's XP is taken into the run, and blocks within which sculk relays join runs. */
    public static final double TAP_RADIUS = 8.0, RELAY_RANGE = 24.0;

    /** Invented. A Data Sensor hears within this many blocks (vanilla's sensor radius) and holds a heard frequency this many ticks (vanilla's cooldown). */
    public static final int DATA_SENSOR_RADIUS = 8, DATA_SENSOR_HOLD_TICKS = 40;

    /** Placeholder. The arc furnace's draw: 42 A at EHV is about 16 kW. */
    public static final float ARC_AMPS = 42f;

    /** Placeholder. Full arc power in watts; the share of it the line delivers sets how far the plasma climbs. */
    public static final float ARC_FULL_WATTS = ElectricTuning.ELECTRIC_EHV_VOLTS * ARC_AMPS;

    /** The hottest the plasma burns whatever the electrode: about 3,500 Tu. */
    public static final Tu ARC_MAX_TU = new Tu(3500f);

    /** Share of an electrode's sublimation (or boiling) temperature its arc can reach. */
    public static final float ARC_PLASMA_OF_SUBLIMATION = 0.97f;

    /** Placeholder. The plasma's thermal mass: small, so it follows the supply closely. */
    public static final ThermalMass ARC_PLASMA_MASS = new ThermalMass(2f);

    /** Placeholder. Condition lost per tick at the electrode's limit (the rate goes with the fourth power of the bath's share of the limit). */
    public static final float ELECTRODE_WEAR_PER_TICK_AT_LIMIT = 0.0004f;

    /** Condition below which an electrode snaps. */
    public static final float ELECTRODE_SNAP_CONDITION = 0.05f;

    /** Invented. Bath temperature at which the arc hearth flashes and splashes, and the radius it reaches. */
    public static final Tu ARC_FLASH_TU = new Tu(3000f);

    public static final float ARC_FLASH_RADIUS = 6f;

    public static final int ARC_FLASH_BLIND_TICKS = 100;

    /** Invented. Chance per tick (while the bath is above {@link #ARC_FLASH_TU} and holds metal) that a splash goes off. */
    public static final float ARC_FLASH_CHANCE = 0.02f;

    /** The arc head's heat reaches every side of its facing neighbour; a plasma is concentrated. */
    public static final float ARC_HEAD_SIDE_SHARE = 1f;

    /** Placeholder. The hottest an induction coil or the forging preheat coil drives its charge: about 1,800 Tu, below the arc. */
    public static final Tu INDUCTION_CEILING_TU = new Tu(1800f);

    /** Placeholder. The induction stove's ceiling: cookware scale, enough to sear a pan, far under a furnace's. */
    public static final Tu INDUCTION_STOVE_CEILING_TU = new Tu(600f);

    /** Placeholder. Current the induction furnace coil, the stove and the preheat coil ask for at MV (6 V): 48 W, 24 W and 48 W. */
    public static final float INDUCTION_COIL_AMPS = 8f, INDUCTION_STOVE_AMPS = 4f, PREHEAT_AMPS = 8f;

    /** Placeholder. Thermal mass of the field a furnace coil or a stove drives: how slowly its heat follows the power. */
    public static final ThermalMass INDUCTION_COIL_MASS = new ThermalMass(40f), INDUCTION_STOVE_MASS = new ThermalMass(20f);

    /** Placeholder. Ticks between a furnace coil looking again at what its crucible holds (the metal search walks the material table). */
    public static final int INDUCTION_CHARGE_CHECK_TICKS = 10;

    /** Placeholder. What the preheat coil adds to a held piece a tick at full power, in Tu per tick (the item cools at 1.5 Tu/t). */
    public static final TuRate PREHEAT_TU_PER_TICK = new TuRate(12f);

    /** Placeholder. The target temperatures a preheat coil steps through, in Tu: off, then up to {@link #INDUCTION_CEILING_TU}. */
    public static final float[] PREHEAT_TARGETS_TU = {0f, 600f, 800f, 1000f, 1200f, 1400f, 1600f, 1800f};

    /** Invented. The step a new preheat coil is set to (an index into {@link #PREHEAT_TARGETS_TU}). */
    public static final int PREHEAT_DEFAULT_TARGET = 3;
}
