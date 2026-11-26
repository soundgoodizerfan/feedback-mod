// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.core.unit.Qu;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.machine.vessel.VesselTuning;
import io.github.soundgoodizerfan.feedback.modular.bake.ModularTuning;

/**
 * Tuning constants for the machine code.
 * Every figure is a placeholder or invented unless its comment says it comes from the slice design.
 */
public final class MachineTuning {

    private MachineTuning() {
    }

    // --- sources --------------------------------------------------------------------------

    /** SLICE: the Hand Crank supplies 12 Su while somebody is turning it. */
    public static final Su HAND_CRANK_CAPACITY_SU = new Su(12f);

    /** Invented. */
    public static final Rpm HAND_CRANK_RPM = new Rpm(32f);

    /** Invented. How long one click keeps the crank turning; holding the button refreshes it. */
    public static final int HAND_CRANK_TICKS_PER_TURN = 12;

    /** SLICE: the Water Wheel supplies 256 Su, continuously, given flowing water. */
    public static final Su WATER_WHEEL_CAPACITY_SU = new Su(256f);

    /** Invented. Speed scales with how many faces have moving water against them. */
    public static final Rpm WATER_WHEEL_RPM_PER_FLOW = new Rpm(4f);

    /** Invented. Water does not change often enough to justify checking every tick. */
    public static final int WATER_WHEEL_FLOW_CHECK_INTERVAL = 20;


    // --- transmission ---------------------------------------------------------------------

    /**
     * Invented. Bearing friction per shaft, in Su per RPM.
     *
     * <h3>Why loss is Su, and why it scales with speed</h3>
     * Loss is Su rather than RPM because a rigid shaft turns at one speed along its whole length
     * -- a difference between its ends is torsion, not loss -- and what friction consumes is
     * torque. Because Su sums network-wide, loss is automatically distance-independent: moving a
     * machine nearer the generator saves nothing, since every bearing in the run turns either
     * way, including those on a branch nobody uses. Sprawl costs; distance does not.
     * <p>
     * Scaling with speed is what makes it interesting rather than a tax. Three things follow,
     * none of which had to be designed:
     * <ul>
     *   <li><b>A stopped shaft costs nothing.</b> An idle network is free, and an unpowered one
     *       is not "overstressed" -- it simply has no load.</li>
     *   <li><b>The network finds its own top speed.</b> It accelerates until surplus torque runs
     *       out, at {@code capacity = drag x rpm}. A long run does not hit a wall and refuse; it
     *       just turns more slowly, because friction ate the torque. This is deliberately unlike
     *       a hard per-shaft Su cap, which produces the absurdity of a generator being <em>too
     *       good</em> for its own shafting.</li>
     *   <li><b>Slow and wide becomes a real alternative to fast and narrow</b>, because speed is
     *       now something you pay for by the block.</li>
     * </ul>
     */
    public static final Drag SHAFT_DRAG_SU_PER_RPM = new Drag(0.1f);

    /**
     * Invented. A bearing is a bushed, greased Shaft variant -- `TODO.md`'s own "obvious first
     * physical upgrade" and nothing more than that: same block shape and role as a Shaft, lower
     * drag. Philosophy 5: point at the bearing, not a percentage -- so this is a second block and
     * a second constant, not a shaft-wide multiplier that would apply everywhere at once and
     * remove the reason to place one over the other.
     * <p>
     * Inertia is untouched (see {@link RotationTuning#SHAFT_INERTIA}) -- a bearing reduces friction, not mass.
     */
    public static final Drag BEARING_DRAG_SU_PER_RPM = new Drag(0.04f);

    /**
     * Placeholder. A bearing is the support a shaft turns in: a run of plain shafts (a flywheel counts as one) longer than this many blocks
     * between bearings or machines is unsupported, and each shaft in it drags {@link #SHAFT_UNSUPPORTED_DRAG_FACTOR} times as much.
     */
    public static final int SHAFT_SUPPORT_SPAN = 4;

    /** Placeholder. What an unsupported shaft's drag is multiplied by. */
    public static final float SHAFT_UNSUPPORTED_DRAG_FACTOR = 2f;

    /**
     * Invented. A cog drags like a shaft; a large one drags half again as much.
     *
     * <h3>What gearing costs</h3>
     * Nothing beyond this, and deliberately. Friction and mass are already referred through the
     * square of the gear ratio (see {@link io.github.soundgoodizerfan.feedback.core.rotation.RotationNetwork}),
     * so a geared-up branch charges the network more simply by turning faster -- and a gear train
     * costs cogs, space, and the drag of every one of them. A real gearbox takes no continuous
     * payment for the force it gives either; what it takes is capital. Adding a surcharge on top
     * would be a rule saying what the physics already says.
     */
    public static final Drag SMALL_COG_DRAG_SU_PER_RPM = new Drag(0.1f);

    public static final Drag LARGE_COG_DRAG_SU_PER_RPM = new Drag(0.15f);

    /** Invented. A large cog is a heavier wheel, and the only flywheel beat 1 has. */
    public static final Inertia SMALL_COG_INERTIA = new Inertia(3f);

    public static final Inertia LARGE_COG_INERTIA = new Inertia(12f);

    /** Invented. The density the base cog inertia figures are for (a wooden cog): a cog cut from a denser metal has inertia in proportion. */
    public static final float COG_REFERENCE_DENSITY = 0.7f;

    /** Invented. Su the Gear Cutter's wheel draws while turning. */
    public static final io.github.soundgoodizerfan.feedback.core.unit.Su GEAR_CUTTER_LOAD_SU = new io.github.soundgoodizerfan.feedback.core.unit.Su(30f);

    /** Invented. The hardest plate the Gear Cutter's wheel bites; a harder one wears the machine by the excess. */
    public static final io.github.soundgoodizerfan.feedback.core.unit.St GEAR_CUTTER_ST = new io.github.soundgoodizerfan.feedback.core.unit.St(30f);

    /** Invented. The share of the plate a 32-tooth cog keeps; fewer teeth keep proportionally less. */
    public static final float GEAR_CUTTER_KEPT_AT_MOST = 0.8f;

    /** Invented. Swarf a plate sheds if none of it were kept. */
    public static final float GEAR_CUTTER_SWARF_PER_PLATE = 4f;

    /** Invented. A gearbox is a crowded little box of bevels: more friction than a bare shaft. */
    public static final Drag GEARBOX_DRAG_SU_PER_RPM = new Drag(0.2f);

    public static final Inertia GEARBOX_INERTIA = new Inertia(4f);

    /** Radius of a winch's drum in blocks: the line draws in {@code RPM x 2 pi x radius} blocks a minute. Placeholder. */
    public static final float WINCH_DRUM_RADIUS = 0.5f;
    /** Su a winch's shaft carries per Mu hanging on the line per block of drum radius (the torque the load puts on the drum); over what the network can supply it stalls. Placeholder. */
    public static final float WINCH_SU_PER_MU_BLOCK = 0.6f;

    /** Placeholder. A limited-slip differential lets its axles' speed shares differ from even by at most this fraction of the input's. */
    public static final float DIFFERENTIAL_SLIP_LIMIT = 0.3f;

    /** Placeholder. Fraction of the way a differential's share moves toward what the loads ask, per tick: smooth, so a wheel losing grip hands over its speed over a few ticks. */
    public static final float DIFFERENTIAL_SHARE_RATE = 0.2f;

    /**
     * Placeholders. The Rotary Bearing and Ring Gear ({@code docs/dev/vehicle.md}, bearings):
     * a gearbox's drag and mass for the housing; the biggest body one turns (Lift's figure is 4096,
     * a bearing's load is an arm, not a car); and the turned body's moment of inertia (kpg x block^2)
     * counted on the run as a flywheel would be.
     */
    public static final Drag ROTARY_BEARING_DRAG_SU_PER_RPM = new Drag(0.2f);

    public static final Inertia ROTARY_BEARING_INERTIA = new Inertia(4f);

    /**
     * Placeholders. The bearing's speed drive (Joint): how fast, per second, the body's spin
     * closes on the run's speed (a first-order lag, solved exactly, so stable at any step); the most
     * torque it applies (kpg x block^2 / s^2), so a jammed body is not wound up; and the least inertia
     * it assumes, so a body Sable has not weighed yet still gets a pull.
     */
    public static final double ROTARY_BEARING_DRIVE_RATE = 20;

    public static final double ROTARY_BEARING_MAX_TORQUE = 5000;

    public static final double ROTARY_BEARING_MIN_INERTIA = 0.05;

    /**
     * Placeholders. How fast a bearing may change its arm's speed against its parent, rad/s^2 (16 RPM
     * is reached in about half a second); and how fast its reaction may spin a parent body, rad/s^2,
     * as a share of that body's own inertia -- so a light car is twisted gently enough for its weight
     * and the ground to hold it, and its arm spins up as slowly as that demands.
     */
    public static final double ROTARY_BEARING_MAX_ACCEL = 4;

    public static final double ROTARY_BEARING_MAX_PARENT_ACCEL = 0.5;

    public static final float ROTARY_BEARING_INERTIA_PER_KPG_BLOCK2 = 0.5f;

    /**
     * Placeholder (invented). The yield strength at which a coupling sleeve's metal carries a rotary bearing's whole torque cap;
     * a weaker metal carries yield over this share of it (iron, 7.5, about three fifths), a stronger one the whole cap.
     */
    public static final float COUPLING_REFERENCE_YIELD = 12f;

    /**
     * Placeholders. Hold at rest: a hinge whose drive asks no speed and that turns slower than this
     * (rad/s against its parent) is held at its angle by a position servo of this rate (per second,
     * as {@link #STEER_SERVO_RATE}), its force capped at the torque the same arm gives a kingpin.
     */
    public static final double JOINT_HOLD_BELOW_RAD_PER_S = 0.05;

    public static final double JOINT_HOLD_RATE = 20;

    /**
     * Placeholders. Joint wear, by misuse only: condition lost per tick a hinge drive sits at its
     * torque cap with no relative turning for longer than a start-up takes (a stalled drive); the lowest condition reached; and condition's cut of the torque cap.
     */
    public static final float JOINT_STALL_WEAR_PER_TICK = 0.0005f;

    /** Placeholder. Physics steps a drive must sit at its cap without turning the arm before that counts as a stall (about 5 s). */
    public static final int JOINT_STALL_GRACE_STEPS = 300;

    /** Placeholder. Game ticks a ball, slider or piston must sit forced against its stop before that counts as misuse (5 s). */
    public static final int JOINT_STRAIN_GRACE_TICKS = 100;

    /** Placeholder. A spring wound of a metal whose yield strength is under this rides soft; from {@link #SPRING_STIFF_FROM_YIELD} up, stiff; between, stock. Lead and tin are soft, iron stock, steel stiff. */
    public static final float SPRING_SOFT_BELOW_YIELD = 4f, SPRING_STIFF_FROM_YIELD = 12f;

    /** Placeholder. Condition lost per game tick of forced strain past the grace (a stalled hinge loses about the same per tick). */
    public static final float JOINT_STRAIN_WEAR_PER_TICK = 0.0002f;

    /** Placeholder. Brake pad condition lost per physics step the drive pushes against a held brake past the grace. */
    public static final float BRAKE_PAD_WEAR_PER_STEP = 0.0005f;

    public static final float JOINT_CONDITION_FLOOR = 0.3f;

    /** Placeholder. Half the swing of a ball joint's cone in radians: narrow, stock, wide. */
    public static final float[] BALL_CONE_RAD = {(float) Math.toRadians(20), (float) Math.toRadians(45), (float) Math.toRadians(90)};

    /** Placeholder. Reach either side of rest of a piston, blocks (a slider's is {@link #SPRING_TRAVEL}). */
    public static final float PISTON_TRAVEL = 0.5f;
    /** Placeholder. Staging: the card level (0 to 1) at which a locked joint lets go, and the speed (blocks per second) the held body is parted at. */
    public static final float STAGE_RELEASE_LEVEL = 0.5f, STAGE_KICK_SPEED = 2f;

    /** Placeholder. A piston servo's stiffness rate per second, per unit of reduced mass. */
    public static final double PISTON_SERVO_RATE = 20;

    /** Invented. A generator is a lump of mass too; without this a bare source has no momentum. */
    public static final Inertia HAND_CRANK_INERTIA = new Inertia(4f);

    /** Invented, and deliberately large -- this is why a water wheel coasts so visibly. */
    public static final Inertia WATER_WHEEL_INERTIA = new Inertia(400f);

    /**
     * Placeholder. A flywheel: a water wheel's mass on a shaft, and twice a shaft's drag for the
     * bearing that carries it -- the price of the momentum, paid every revolution.
     */
    public static final Inertia FLYWHEEL_INERTIA = new Inertia(400f);

    public static final Drag FLYWHEEL_DRAG_SU_PER_RPM = new Drag(0.2f);


    // --- reciprocation --------------------------------------------------------------------

    /**
     * Invented. Strokes delivered per RPM per tick, so a water wheel at 8 RPM lands about one
     * blow a second and a hand crank at 32 RPM about four.
     */
    public static final float STROKES_PER_RPM_PER_TICK = 0.00625f;

    /**
     * Placeholder. The share of the seated head's St ceiling a blow carries at each crank setting, before
     * gearing. The head sets the ceiling ({@link ModularTuning#HEAD_ST_PER_MU_COMPETENCE}); the setting picks within it, so
     * both stay meaningful. Half and an eighth keep the old 12 / 3 St against the old 24 St ceiling.
     */
    public static final float HAMMER_THROW_SHARE_SHORT = 0.5f;

    public static final float HAMMER_THROW_SHARE_LONG = 0.125f;

    /** SLICE: 80 Su, against the water wheel's 256. Three hammers fit; four do not. */
    public static final Su HAMMER_LOAD_SU = new Su(80f);

    /**
     * How much condition one St of wasted force costs the machine that absorbed it.
     *
     * <h3>What wear is for here, and what it deliberately is not</h3>
     * It is not maintenance. GregTech charges a flat chance of a fault per hour a machine runs,
     * which is a tax on uptime and says nothing about whether the factory was built well --
     * looked at and rejected for exactly that. Wear here is caused <em>only</em> by misuse, so a
     * correctly built line never accrues any of it, ever. A worn hammer is therefore not an
     * upkeep bill; it is <em>evidence</em>, and the thing it is evidence of is the mistake.
     * <p>
     * The rule it implements is one sentence: force that cannot go into the work goes into the
     * machine. That covers all three ways a blow can accomplish nothing -- the workpiece is too
     * cold to move, the blow is under the material's hardness floor, or the drive is geared past
     * what the hammer's construction can take -- without a separate rule for any of them. Before
     * this, the first two were silently ignored and the third was a silent clamp, which meant the
     * three most instructive mistakes in beat 1 were the three the game said nothing about.
     *
     * <h3>Where the figure comes from</h3>
     * Condition falls from 1 to {@link #HAMMER_CONDITION_FLOOR} after about 600 wasted short-throw
     * blows -- {@code 0.5 / (600 x 12 St)}. A water wheel lands roughly a blow a second, so that
     * is ten minutes of a hammer beating metal that went cold. Long enough that a player who
     * notices and fixes it pays almost nothing, short enough that walking away from a broken setup
     * costs the hammer.
     */
    public static final float HAMMER_WEAR_PER_ST = 0.00007f;

    /**
     * The worst condition a hammer can reach. It never breaks and never stops.
     *
     * <h3>Why it degrades instead of failing</h3>
     * Philosophy 8: a hard gate is a physical impossibility, and a machine refusing to run is one
     * bolted on where none exists. A battered head still hits, it just delivers less of the blow
     * into the work -- so the failure is soft, gradual, and material-dependent. Copper carries on
     * yielding to a spent hammer; steel stops clearing its hardness floor and the line quietly
     * stops producing. Which material notices first is the material's business, which is the same
     * answer this mod gives everywhere else.
     * <p>
     * Half, so a fully spent hammer is worth exactly half a hammer -- bad enough to be the reason
     * a build stopped working, never bad enough to be unrecoverable.
     */
    public static final float HAMMER_CONDITION_FLOOR = 0.5f;


    // --- continuous deformation (machine shop) -----------------------------------------------

    /**
     * Invented. The Wire Drawer's two draw speeds, as a share of the seated die's ceiling -- the same shape as
     * the hand crank's short/long throw (§17 of the philosophy): fast draws with the die's full ceiling at the cost
     * of a per-tick chance the wire snaps outright (see {@link #WIRE_SNAP_CHANCE}); slow is gentle, always safe,
     * and takes about a third of it, so a metal that only the die's full ceiling clears is reached only by
     * accepting the risk, not by waiting it out.
     */
    public static final float WIRE_DRAWER_SLOW_SHARE = 0.325f;

    /** Invented. Chance per landed fast draw that the wire snaps outright (a draw lands about
     * four times a second at 32 RPM, so this is roughly the old 1%-per-tick rate). A
     * genuine loss -- the workpiece is destroyed, not merely wasted a tick's progress, per §7's
     * "failed batches must consume their inputs" applied to a deliberate risk rather than to
     * ignorance. */
    public static final float WIRE_SNAP_CHANCE = 0.05f;

    public static final Su WIRE_DRAWER_LOAD_SU = new Su(30f);

    public static final Su ROLLING_MILL_LOAD_SU = new Su(50f);

    /**
     * [OPEN] Roll cladding: a skin plate lays this much of its metal (Qu) on the base plate. Between the plating
     * porous and spall limits, so a clean bond is a sound coat.
     */
    public static final float CLAD_SKIN_QU = 1f;

    /** [OPEN] Both plates must be past their anneal point to bond (solid-state, no flux); a skin past this share of its melting point is overrun and burnt. */
    public static final float CLAD_OVERRUN_MELT_SHARE = 0.9f;

    // The Press has no St of its own: it lands the seated head's full ceiling each stroke.
    public static final Su MECHANICAL_PRESS_LOAD_SU = new Su(90f);

    /** Invented. Condition a machine-shop body loses per St it absorbs, and where it bottoms out --
     * the Hammer's own figures, shared by every {@code ShopMachineBlockEntity}, since the rule is
     * the same rule. */
    public static final float SHOP_MACHINE_WEAR_PER_ST = HAMMER_WEAR_PER_ST;

    public static final float SHOP_MACHINE_CONDITION_FLOOR = HAMMER_CONDITION_FLOOR;


    // --- removal family (machine shop) -------------------------------------------------------

    /**
     * Invented. Chance that a landed removal cut also drops one of its entry's swarf into
     * the world -- philosophy §8's "failed batches must consume their inputs," read the other way
     * round: the removed mass is real material, not deleted, so a cut is allowed to pay some of
     * itself back. Deliberately probabilistic rather than "one swarf per Wu removed": the exact
     * accounting is a tuning question the spec explicitly defers, and this is the cheapest honest
     * version of "removal produces recoverable scrap."
     */
    public static final float SWARF_DROP_CHANCE = 0.15f;

    /** Invented. The Drill Press's two feed speeds -- slow is always safe, fast risks a bound,
     * snapped bit (see {@link #BIT_SNAP_CHANCE}) but cuts faster. Both clear steel's hardness once
     * the seated bit's edge does; the choice is speed against risk, not
     * possible against impossible, unlike the Wire Drawer's pair. */
    public static final St DRILL_PRESS_ST_SLOW = new St(22f);

    public static final St DRILL_PRESS_ST_FAST = new St(48f);

    /** Invented. Chance per fast-feed push that the bit binds -- a wasted push and a warning
     * sound, not damage to the machine body or the workpiece (§7: a hard gate on the fast setting
     * alone would be a lock; this is a genuine risk instead). */
    public static final float BIT_SNAP_CHANCE = 0.05f;

    public static final Su DRILL_PRESS_LOAD_SU = new Su(35f);

    /** Invented. The threshing drum's load; it threshes from {@link #THRESHER_MIN_RPM}, one plant a {@link #THRESHER_TICKS_PER_PLANT} ticks at {@link #THRESHER_REFERENCE_RPM}
     * (faster in proportion), and flings its seed out of the tray past {@link #THRESHER_FLING_RPM}. */
    public static final Su THRESHER_LOAD_SU = new Su(40f);
    public static final io.github.soundgoodizerfan.feedback.core.unit.Rpm THRESHER_MIN_RPM = new io.github.soundgoodizerfan.feedback.core.unit.Rpm(16f),
            THRESHER_REFERENCE_RPM = new io.github.soundgoodizerfan.feedback.core.unit.Rpm(32f),
            THRESHER_FLING_RPM = new io.github.soundgoodizerfan.feedback.core.unit.Rpm(64f);
    public static final float THRESHER_TICKS_PER_PLANT = 20f;
    /** Invented. Seeds a threshed plant gives (a plant also gives one straw and one chaff). */
    public static final int THRESHER_SEEDS_PER_PLANT = 2;
    /** Invented. What the drum's teeth cut through: harder than this jams it. A jam costs the teeth this much condition per multiple of the excess, never below the floor. */
    public static final io.github.soundgoodizerfan.feedback.core.unit.St THRESHER_TEETH_HARDNESS = new io.github.soundgoodizerfan.feedback.core.unit.St(4f);
    public static final float THRESHER_TEETH_FLOOR = 0.4f, THRESHER_JAM_WEAR = 0.1f;

    /** Invented. Light/heavy pass -- same shape as every other discrete throw in the mod: heavy
     * cuts faster and coarser, light is slower and controllable. Neither risks the bit; only
     * cutting a material the bit's own rating cannot clear wears it (see {@link #SHOP_MACHINE_WEAR_PER_ST}). */
    public static final St LATHE_ST_LIGHT = new St(18f);

    public static final St LATHE_ST_HEAVY = new St(40f);

    public static final Su LATHE_LOAD_SU = new Su(40f);

    /**
     * Invented. The off-measure share of a Lathe's cut with no screw seated: none, a bare carriage cuts as it always did. A seated screw feeds
     * more steadily ({@link #LATHE_SCREW_FEED_GAIN}) less its backlash, rolled once for each piece chucked: a cut screw has little, a rolled
     * screw more. A harder screw metal flexes less
     * ({@link #SCREW_REFERENCE_HARDNESS}, the scale clamped to {@link #SCREW_METAL_SCALE_MIN} to {@link #SCREW_METAL_SCALE_MAX}).
     */
    public static final float LATHE_BARE_FEED_ERROR = 0f;
    /** Invented. What a seated screw adds to the drive of every cut, before its backlash is taken off: a lead screw feeds more steadily than a carriage slid by hand. */
    public static final float LATHE_SCREW_FEED_GAIN = 0.04f;
    public static final float SCREW_CUT_LOOSENESS = 0.01f, SCREW_ROLLED_LOOSENESS = 0.035f;
    public static final St SCREW_REFERENCE_HARDNESS = new St(15f);
    public static final float SCREW_METAL_SCALE_MIN = 0.5f, SCREW_METAL_SCALE_MAX = 2f;

    public static final Su GRINDING_WHEEL_LOAD_SU = new Su(45f);

    /**
     * Invented. Condition lost per Wu ground, under *correct* use -- the stated exception to the
     * Hammer's "wear only from misuse" rule. A real grinding wheel sheds grit whether or not the
     * cut was a mistake, so this fires on every landed grind, not on a failed one -- see {@code
     * GrindingWheelBlockEntity}'s own class doc for why this diverges from the Lathe and the
     * Hammer. Coarse wastes more grit per Wu than fine, matching real abrasive wear.
     */
    public static final float GRINDING_WHEEL_WEAR_PER_WU = 0.003f;

    /** The wheel is consumed to nothing: its condition floor is zero, and a bald wheel has no hardness left to cut with. */
    public static final float GRINDING_WHEEL_CONDITION_FLOOR = 0f;

    /** Invented. Tu added to the workpiece per Wu ground, proportional and small -- real
     * interface heat, not a process. Lets overrun grinding a hardened part re-soften it by
     * pushing it back into a tempering-relevant range, which needs no new mechanic: {@code
     * ItemHeat} already treats every workpiece this way regardless of how it got hot. */
    public static final float GRINDING_HEAT_PER_WU = 0.4f;

    public static final Drag PULLEY_DRAG_SU_PER_RPM = new Drag(0.1f);

    public static final Inertia PULLEY_INERTIA = new Inertia(3f);

    public static final ThermalMass CRUCIBLE_LARGE_MASS = new ThermalMass(250f);


    // --- the boiler (thermal -> steam) -------------------------------------------------------

    /**
     * Invented. First bridge between two energy types (philosophy §11, §14): fire in, Steam out
     * -- and nothing else, deliberately, because the whole point of the split below is that this
     * block has no idea rotation exists.
     *
     * <h2>Two blocks, not one, and that is the design decision this section records</h2>
     * The first draft of this was one block, boiler and turbine in the same body, on the theory
     * that {@code ThermalVesselBlockEntity} already makes exactly that simplification over the
     * crucible-plus-firebox pair. That is wrong here in a way it is not there: a furnace is
     * always its own fire, but "what makes the heat" is precisely the axis philosophy §11 says
     * must stay open. A single fused block can only ever be fired by whatever the class author
     * imagined, and this system exists so the *player* answers that question -- a firebox, a
     * geothermal vent, a resistive coil wired to a generator that is itself downstream of this
     * same engine, run backwards, badly, on purpose. Nothing should stop that chain except its
     * own thermodynamics telling the player it is a bad idea. A fused block forecloses it by
     * construction; two blocks joined only by a real, physical fluid do not.
     * <p>
     * So {@link io.github.soundgoodizerfan.feedback.machine.boiler.BoilerBlockEntity} is a
     * {@link ThermalBody} and nothing else -- it does not know a
     * {@link io.github.soundgoodizerfan.feedback.core.rotation.RotationNode} exists -- and
     * {@link io.github.soundgoodizerfan.feedback.machine.steamengine.SteamEngineBlockEntity} is a
     * {@code RotationNode} and nothing else -- it does not know what a Tu is. Steam, a registered
     * {@link io.github.soundgoodizerfan.feedback.registry.FFluids#STEAM Fluid} with a real tank
     * on each side, is the only thing that crosses between them, exactly the way {@code Work}
     * crosses a {@link Conductance} boundary and neither side needs to know what is on the other.
     *
     * <h3>Read GregTech CEu Modern's boiler and turbine first (LGPL-3.0, design only)</h3>
     * `../GregTech-Modern-7.5.3`, `1.21.1`-targeting branch checked out. See
     * `THIRD-PARTY-LICENSES.md` for the full account; briefly:
     * <ul>
     *   <li><b>Taken, and why it now matters more than it first looked like it would:</b> GTCEu
     *       already keeps its boiler and turbine as separate multiblocks joined only by a Steam
     *       fluid moving through hatches. That split was read once for "how do I make steam a
     *       real thing" and is the reason this is two blocks rather than one -- the puzzle-piece
     *       argument above is Feedback's own reason for keeping the split, not GTCEu's, but GTCEu
     *       is proof the split is buildable rather than a nice idea that falls apart on contact
     *       with a tick loop.</li>
     *   <li><b>Taken:</b> a boiler is a {@code ThermalBody} whose temperature gates steam
     *       production -- {@code LargeBoilerMachine#updateCurrentTemperature} drains water and
     *       fills steam only once its own temperature clears a floor, the same shape
     *       {@link #BOILER_WORKING_TU} enforces here.</li>
     *   <li><b>Taken:</b> a turbine's rotor ramps speed up and down rather than snapping to it --
     *       {@code RotorHolderPartMachine}'s asymmetric accel/decel is the same shape this
     *       codebase already has in {@link io.github.soundgoodizerfan.feedback.core.rotation.RotationNetwork}'s
     *       inertia, so nothing new had to be invented for the engine side, only wired up.</li>
     *   <li><b>Taken, and deferred:</b> the rotor as a physical, damageable, swappable item
     *       ({@code TurbineRotorBehaviour}) rather than a machine stat -- philosophy's "upgrades
     *       are physical components" arriving independently in their design too. Not built this
     *       pass; {@link VesselTuning#STEAM_ENGINE_CAPACITY_SU} stands in for a rotor that does not exist
     *       yet, the same way {@link #HAND_CRANK_CAPACITY_SU} once stood in for gearing.</li>
     *   <li><b>Not taken: EU as the output.</b> GTCEu's turbine produces electricity because
     *       everything in GTCEu eventually does. Feedback's point of departure (§10) is that
     *       Mechanical never has to become Electrical, so the engine's output is {@link Su}/
     *       {@link Rpm} on a plain {@code RotationNode}, full stop -- a flywheel and a belt, not
     *       a generator. Nothing stops a later Electrical-to-Thermal adapter feeding this same
     *       boiler, which is exactly the loop-back the class doc above names.</li>
     *   <li><b>Taken, later (2026-10-14): the exploding boiler.</b> `feedback_philosophy.md` §15
     *       names it as good precedent for a genuine hard-gate consequence. It was left out first
     *       because the boiler could not be over-restricted -- there was no failure state to
     *       explode into. It now has a pressure ({@link VesselTuning#BOILER_STEAM_MB_PER_PU},
     *       {@link VesselTuning#BOILER_PU_PER_TU_OVER}): a shut line under a real fire is the only way there,
     *       a weeping, scalding boiler gives warning first, and the ladder is the Pressure
     *       Vessel's own (`PressureBurst`).</li>
     * </ul>
     */
    public static final Tu BOILER_WORKING_TU = new Tu(120f);

    /**
     * Invented. How fast the boiler bleeds heat to the room when it is not making steam, in the
     * same {@code Work/t/Tu} every other leak is in. An ordinary vessel leak, {@link ThermalTuning#VESSEL_LEAK}
     * -- the boiler is not an engine and has nothing analogous to an engine's exhaust; venting
     * heat as work happens explicitly, below, as mB of Steam, never through this figure.
     */
    public static final Conductance BOILER_LEAK = ThermalTuning.VESSEL_LEAK;

    /**
     * Invented, placeholder. The three springs an overspeed trip can be fitted with, light to
     * heavy; index 0 is {@link FTuning#OVERSPEED_TRIP_RPM}, the spring it is built with. The light one
     * suits hand and engine trains geared up, the heavy one lets a 240 RPM turbine run.
     */
    public static final float[] OVERSPEED_TRIP_SPRINGS_RPM = {48f, 120f, 300f};

    /** Invented. How fast the pot's heat closes on its target, per tick. */
    public static final float POT_RELAX_PER_TICK = 0.02f;

    public static final Tu ICEBOX_COLD_TU = new Tu(2f);

    /** Invented. The drying rack: moisture lost per tick at ambient (it scales with the air's heat), the hottest the air may be before food would cook, and the most food it holds. */
    public static final float RACK_DRY_PER_TICK = 4.7e-5f;

    public static final Tu RACK_MAX_AIR_TU = new Tu(65f);

    public static final int RACK_SLOTS = 4;

    /**
     * The Mixer's drum (guns spec 13.4b). Placeholders. Turns to blend a load to uniform; past that
     * the drum grinds the finished blend, so the grains break down (uniformity falls toward its
     * floor over {@code MIXER_SEGREGATE_TURNS}) and each turn adds {@code MIXER_FRICTION_TU} of heat.
     * Heat above ambient reaching the blend's lowest ignition temperature sets it off.
     */
    public static final float MIXER_BLEND_TURNS = 40f, MIXER_SEGREGATE_TURNS = 60f, MIXER_UNIFORMITY_FLOOR = 0.5f,
            MIXER_FRICTION_TU = 0.5f;

    public static final Su MIXER_LOAD_SU = new Su(20f);

    /** The drum as a thermal body: its mass, and its leak (an open drum loses heat faster than a closed vessel). Invented. */
    public static final ThermalMass MIXER_MASS = new ThermalMass(20f);

    public static final Conductance MIXER_LEAK = new Conductance(0.04f);

    /** Cells in a galvanic pack (zinc against copper, 1.1 V each): 5.5 V, under the MV reference. */
    public static final int GALVANIC_PACK_CELLS = 5;

    public static final float PLATING_SPALLS_ABOVE_QU = 1.8f;

    /** Invented. Explosion power of a boiler that lets go, the Pressure Vessel's own figure. */
    public static final float BOILER_BURST_POWER = 2.5f;

    /**
     * Placeholder. How far a steered hinge turns at full lock, either way, in radians (about 35
     * degrees, a real car's road-wheel lock). The steering level -1 to 1 is a fraction of it.
     */
    public static final float STEER_MAX_ANGLE = 0.6f;

    /**
     * Invented. The driver's arm at a kingpin, with no assist: a few hundred newton-metres, a
     * person on a wheel through a real steering box. It is the servo's force limit, so steering
     * load is not a rule: a tire scrubbing in place resists with its grip times its load times
     * the scrub lever, about a kilonewton-metre on a tonne of car, and the arm loses -- parked,
     * the wheels barely turn. Rolling, the tire turns by rolling and the arm wins (spec section 5,
     * <em>Steering</em>).
     */
    public static final Su STEER_ARM_SU = new Su(200f);

    /**
     * Invented. The steering servo's stiffness and damping per unit of the knuckle's inertia
     * about the kingpin, as {@code rate^2} and {@code 2 x ratio x rate}: so a heavy knuckle
     * settles as fast as a light one, and the arm (above) is the only limit that shows. The shape
     * is Create Simulated's swivel bearing (MIT; see {@code THIRD-PARTY-LICENSES.md}).
     */
    public static final float STEER_SERVO_RATE = 40f;

    public static final float STEER_SERVO_DAMPING_RATIO = 0.7f;

    /**
     * Placeholder. The share of its turning rate a free hinge or ball takes off the swinging body each
     * second (1/s): an arm let go comes to rest in a few seconds rather than swinging for ever. A
     * bearing is not frictionless, and this is Create Simulated's unpowered-swivel friction (MIT)
     * without a stiffness; it is a rate on the body's inertia, so a heavy arm settles as fast as a light one.
     */
    public static final float BEARING_DAMPING_RATE = 2f;

    /**
     * The slider spring's travel either side of rest, in blocks (vehicles spec, differential, slider
     * and transmission). Placeholder.
     */
    public static final float SPRING_TRAVEL = 0.25f;

    /** Invented. The casting basin: a block's worth of pool, the share of the gap to ambient it cools each tick, and mB it takes from a vessel each tick. */
    public static final int BASIN_POOL_MB = 1296, BASIN_POUR_MB_PER_TICK = 18;

    public static final float BASIN_COOL_PER_TICK = 0.002f;

    /** Placeholder. Four times a large crucible: the hearth is a deep, heavily lined bath. */
    public static final ThermalMass ARC_HEARTH_MASS = new ThermalMass(1000f);

    public static final int ARC_CHARGE_SLOTS = 6, ARC_ELECTRODE_SLOTS = 3;

    /** Placeholder. Current the Power Cutter asks for while it has a head and a piece to cut: about 1 kW at EHV (384 V), 2.6 A. */
    public static final float CUTTER_AMPS = 2.6f;

    /**
     * Placeholder. A circuit in a data-stick machine (Power Cutter, Auto Engineer): playback runs {@code 1 + PACE_GAIN x speed} times as fast,
     * and the odds of a bad outcome on input the stick does not cover scale by {@code 1 - ACCURACY_CUT x (1 - noise)} (with a hold-off that
     * often per cycle). No circuit changes nothing.
     */
    public static final float CIRCUIT_PACE_GAIN = 1f, CIRCUIT_ACCURACY_CUT = 0.6f;

    /** Placeholder. Cuts per second at full power with a sound head (spec: about two). */
    public static final float CUTTER_CUTS_PER_SECOND = 2f;

    /** The ticks of full-power running one cut takes: 10 at two cuts a second. */
    /** A waterjet head: cuts' worth of grit one piece of grit stock loads, the most a head holds, the least abrasiveness a grit needs, and nozzle wear units a cut spends per point of the grit's abrasiveness. */
    public static final int WATERJET_GRIT_PER_PIECE = 8, WATERJET_GRIT_MAX = 64;
    public static final float WATERJET_GRIT_MIN_ABRASIVENESS = 0.5f, WATERJET_NOZZLE_WEAR_PER_ABRASIVENESS = 0.6f;
    /** The cutter's water tank (mB) and the water one waterjet cut drives through the nozzle (mB). Placeholder. */
    public static final int WATERJET_TANK_MB = 2000, WATERJET_WATER_PER_CUT = 25;

    /** The membrane sensor's reach (blocks), and the sleeplessness (ticks since rest) that calls phantoms, which reads as 1. */
    public static final int MEMBRANE_REACH = 8, MEMBRANE_PHANTOM_TICKS = 72000;

    // --- the laser (roadmap O4); placeholder figures, none tuned ---
    /** Blocks the beam reaches. */
    public static final int LASER_RANGE = 24;
    /** Amps the pump draws on an MV line while it only ranges, and while it fires. */
    public static final float LASER_RANGING_AMPS = 0.1f, LASER_FIRING_AMPS = 1.0f;
    /** Dose a tick of beam puts into the block it hits, per unit of crystal clarity; a block is cut when its dose reaches hardness times {@code LASER_DOSE_PER_HARDNESS}. */
    public static final float LASER_DOSE_PER_TICK = 0.5f, LASER_DOSE_PER_HARDNESS = 40f;
    /** Heat the crystal gains a firing tick: the base, plus a share of this for what the crystal turns back (one minus its clarity); it sheds {@code LASER_COOL_PER_TICK} a tick not firing. It cracks at {@code LASER_CRACK_HEAT}. */
    public static final float LASER_HEAT_PER_TICK = 1f, LASER_HEAT_LOSS_SHARE = 4f, LASER_COOL_PER_TICK = 0.6f, LASER_CRACK_HEAT = 2400f;
    /** Reflectivity of a crystal whose material states none. */
    public static final float LASER_DEFAULT_REFLECTIVITY = 0.1f;
    /** Hurt a living thing in the beam takes every {@code LASER_HURT_EVERY} ticks, and the seconds it burns. */
    public static final float LASER_HURT = 3f;
    public static final int LASER_HURT_EVERY = 10, LASER_BURN_SECONDS = 3;
    public static final float CUTTER_TICKS_PER_CUT = 20f / CUTTER_CUTS_PER_SECOND;

    /** Placeholder. The Power Cutter's input buffer, in items (one stack). */
    public static final int CUTTER_INPUT_LIMIT = 64;

    /** Placeholder. Condition one unit of misuse costs a cutter head (a blocked output, over-voltage or stock too hard for it, the last scaled by how much harder). */
    public static final float CUTTER_WEAR_PER_MISUSE = 0.002f;

    /** Placeholder. Units of misuse per cut for each unit of fraction the supply is over the cutter's EHV rating (0.1 over costs 5 units). */
    public static final float CUTTER_OVERVOLT_WEAR_UNITS = 50f;

    /** Placeholder. A worn cutter head bottoms out here: it cuts at a quarter of its rate and never stops. */
    public static final float CUTTER_CONDITION_FLOOR = 0.25f;

    /** Placeholder. Relative odds, per cut of input the stick does not cover: the seeded cut, a jam, blade wear, scrap of the material. Bad end of the mix for an early machine. */
    public static final float CUTTER_SEEDED_ODDS = 4f, CUTTER_JAM_ODDS = 2f, CUTTER_BLADE_WEAR_ODDS = 2f, CUTTER_SCRAP_ODDS = 2f;

    /** Placeholder. Units of misuse a blade-wearing bad cut costs (stock too hard for the head costs its hardness ratio, about 1 to 3). */
    public static final float CUTTER_BLADE_WEAR_UNITS = 3f;

    /** Placeholder. Hardness assumed for a cutter head that carries no material. */
    public static final float CUTTER_DEFAULT_HEAD_HARDNESS = 30f;

    /** Placeholder. Current the Auto Engineer asks for while it has parts to work: about 1 kW at EHV (384 V). */
    public static final float ENGINEER_AMPS = 2.6f;

    /** Placeholder. Ticks of full-power running one assembly (or one scrapping) takes: three seconds. */
    public static final float ENGINEER_TICKS_PER_ASSEMBLY = 60f;

    /** Placeholder. Condition one unit of misuse costs an Auto Engineer (stock too hard, a full output, over-voltage, scrapping). */
    public static final float ENGINEER_WEAR_PER_MISUSE = 0.002f;

    /** Placeholder. Units of misuse per assembly for each unit of fraction the supply is over the Auto Engineer's EHV rating. */
    public static final float ENGINEER_OVERVOLT_WEAR_UNITS = 50f;

    /** Placeholder. A worn Auto Engineer bottoms out here: it works at a quarter of its rate and never stops. */
    public static final float ENGINEER_CONDITION_FLOOR = 0.25f;

    /** Placeholder. The hardest stock (a material's hardness) the Auto Engineer's construction takes; harder stock is spoiled and wears it by the ratio. */
    public static final float ENGINEER_STOCK_HARDNESS = 60f;

    /** Placeholder. Share of its mass a scrapped input keeps as nuggets: the rest is lost. */
    public static final float ENGINEER_SCRAP_YIELD = 0.5f;

    /** Invented. Gas centrifuge: least RPM that separates anything, ingots and hydrofluoric acid a batch takes, ticks a batch takes at the least RPM (faster spins finish sooner, by the square), and its shaft load. */
    public static final float CENTRIFUGE_MIN_RPM = 24f;

    public static final int CENTRIFUGE_BATCH_INGOTS = 4, CENTRIFUGE_HF_MB = 50, CENTRIFUGE_BATCH_TICKS = 1200, CENTRIFUGE_HF_TANK_MB = 400;

    public static final Su CENTRIFUGE_LOAD_SU = new Su(60f);

    /**
     * Placeholder. Scrubbing bed (flux in Thaumaturge's own units): flux a seated bed of full activity takes out of its chunk a second, flux it
     * holds before it is saturated, flux a saturated bed lets back into the chunk a second, the share of its load a fire at its regeneration
     * temperature burns off a second, and the share of its activity a fire at its sinter temperature takes for good a second.
     */
    public static final float SCRUBBER_FLUX_PER_SECOND = 1.5f, SCRUBBER_CAPACITY = 120f, SCRUBBER_BREAKTHROUGH_PER_SECOND = 0.5f,
            SCRUBBER_REGEN_PER_SECOND = 0.04f, SCRUBBER_SINTER_PER_SECOND = 0.02f;

    /**
     * Placeholder. Essentia vent: the essentia held (Thaumaturge's own units; its reservoir holds 256) at which the valve opens and below which it
     * reseats, the essentia it bleeds a second while open, and the flux that each vented essentia becomes in the chunk (its reservoir's own rupture figure).
     */
    public static final int ESSENTIA_VENT_ABOVE = 180, ESSENTIA_VENT_RESEAT = 120, ESSENTIA_VENT_PER_SECOND = 8;

    public static final float ESSENTIA_VENT_FLUX_PER_ESSENTIA = 0.25f;

    /** Placeholder [OPEN]. A seated Nether Star's constant heat (above the arc's cap, enough to melt tungsten) and how many loaded ticks it burns before it is spent (about 20 hours). */
    public static final io.github.soundgoodizerfan.feedback.core.unit.Tu STAR_BURNER_TU = new io.github.soundgoodizerfan.feedback.core.unit.Tu(3800f);
    public static final long STAR_BURNER_LIFE_TICKS = 1_440_000L;

    /** Placeholder [OPEN]. Fusion chamber: tank size (mB), the temperature something beside it must reach to light it, the plasma's temperature when held and the most heat it gives on (Tu), how slowly the plasma follows its target (Work per Tu), hydrogen burned a tick (mB) and helium made per mB of it, the field's draw (A) and the share of it that holds the plasma, the wall's wear a tick with the field lost and the blast when it is gone. */
    public static final int FUSION_TANK_MB = 4000;
    public static final io.github.soundgoodizerfan.feedback.core.unit.Tu FUSION_IGNITION_TU = new io.github.soundgoodizerfan.feedback.core.unit.Tu(3700f),
            FUSION_PLASMA_TU = new io.github.soundgoodizerfan.feedback.core.unit.Tu(9000f),
            FUSION_OUTPUT_TU = new io.github.soundgoodizerfan.feedback.core.unit.Tu(4500f);
    public static final float FUSION_PLASMA_MASS = 200f, FUSION_BURN_MB_PER_TICK = 2f, FUSION_HELIUM_PER_HYDROGEN = 0.25f,
            FUSION_FIELD_AMPS = 12f, FUSION_HOLD_SHARE = 0.9f, FUSION_WALL_WEAR_PER_TICK = 0.0008f, FUSION_BURST_POWER = 6f;

    /** Placeholder [OPEN]. A control surface's full deflection, in radians (about 29 degrees); the arm's force limit is the driver's (STEER_ARM_SU). */
    public static final float SURFACE_MAX_ANGLE = 0.5f;
}
