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
package io.github.soundgoodizerfan.feedback.core;

import java.util.List;

import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.core.unit.Mb;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.unit.Qu;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.TuRate;

/**
 * Every tunable number in the mod, in one place.
 *
 * <h2>Why this file exists</h2>
 * Almost none of these figures are defensible yet. Slice 1 fixes {@code Su} for a few machines
 * and never fixes a speed at all, and the philosophy is explicit that exact values are not
 * decided. They will stay fiction until the slice has been played.
 * <p>
 * So the point of centralising them is not tidiness, it is <em>turnaround</em>. Tuning should be
 * editing one list, not hunting through a dozen block entities. When the numbers start to mean
 * something, this becomes a config file; until then it is the single place a playtest note can
 * be applied without reading any code.
 * <p>
 * Anything here marked SLICE comes from docs/feedback_slice_01.md. Everything else is invented.
 */
public final class FTuning {

    private FTuning() {
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
     * drag. Philosophy 4: point at the bearing, not a percentage -- so this is a second block and
     * a second constant, not a shaft-wide multiplier that would apply everywhere at once and
     * remove the reason to place one over the other.
     * <p>
     * Inertia is untouched (see {@link #SHAFT_INERTIA}) -- a bearing reduces friction, not mass.
     */
    public static final Drag BEARING_DRAG_SU_PER_RPM = new Drag(0.04f);

    /**
     * Invented. How much a shaft resists a change in speed.
     *
     * <h3>How the inertia numbers were picked</h3>
     * Acceleration is net torque over moment of inertia -- {@code (capacity - load) / inertia}
     * RPM per tick -- which is the real relationship and not a fudge. That means the inertia
     * figures have to be calibrated against the Su figures, or momentum is invisible: at the
     * first values tried, a water wheel reached full speed in a single tick.
     * <p>
     * They are set so that a water wheel takes roughly two seconds to come up to speed and a
     * hand crank a little under one. The spread between them is large, and correctly so: a water
     * wheel is an enormous slab of timber and a crank handle is not.
     */
    public static final Inertia SHAFT_INERTIA = new Inertia(2f);

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

    /** Invented. A gearbox is a crowded little box of bevels: more friction than a bare shaft. */
    public static final Drag GEARBOX_DRAG_SU_PER_RPM = new Drag(0.2f);
    public static final Inertia GEARBOX_INERTIA = new Inertia(4f);

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
    /**
     * Placeholder (user ruling, 2026-10-23). Rim speed limit of a flywheel is this times the root of its metal's yield
     * strength over density (cast iron comes to about 40 RPM, under the overspeed trip). Past it the rim strains:
     * condition falls by {@code FLYWHEEL_WEAR_PER_OVER} per tick per unit of fractional overspeed, to the floor and no
     * further. Never a burst.
     */
    public static final float FLYWHEEL_RIM_RPM_PER_ROOT = 44f, FLYWHEEL_WEAR_PER_OVER = 0.001f, FLYWHEEL_CONDITION_FLOOR = 0.5f;

    /**
     * Floor on total network inertia, so a network of one weightless block still takes a moment
     * to change speed instead of snapping. Prevents a divide-by-zero as well.
     */
    public static final Inertia MINIMUM_INERTIA = new Inertia(1f);

    /**
     * Global multiplier on how quickly anything changes speed. 1 means the physics above is used
     * as written.
     * <p>
     * Raise it and machines feel light and twitchy; lower it and everything runs as though in
     * oil. This is the first number to reach for when the mod feels wrong but nothing specific
     * is wrong with it.
     */
    public static final float INERTIA_RESPONSE = 1f;

    /**
     * Floor on braking force, so an utterly frictionless network still eventually stops rather
     * than coasting for the rest of the save.
     */
    public static final Su MINIMUM_BRAKING_SU = new Su(0.5f);

    // --- reciprocation --------------------------------------------------------------------

    /**
     * Invented. Strokes delivered per RPM per tick, so a water wheel at 8 RPM lands about one
     * blow a second and a hand crank at 32 RPM about four.
     */
    public static final float STROKES_PER_RPM_PER_TICK = 0.00625f;

    /**
     * SLICE: the Mechanical Hammer delivers 12 St on a short throw and 3 on a long one.
     * <p>
     * Note there is deliberately no matching pair for work. A lever trades force against
     * distance and the work per stroke is the same either way, so throw changes how hard a blow
     * lands and not how fast the job goes. That is exactly why copper -- which yields to almost
     * nothing -- lets a player install the wrong crank and never find out.
     */
    /**
     * SLICE: a hand hammer's ordinary swing is about 3 St ({@code HandSwing.BLOW}, the default).
     *
     * <p>Against copper's hardness of 1 that is 3 Fu a swing, so a 14 Fu plate is five swings and
     * the sixth starts making foil. It is deliberately the same figure as the Mechanical Hammer's
     * long throw: the gentle machine is doing exactly what the player's arm was doing, only without
     * ever getting bored, which is the whole of what beat 1 has to say.
     *
     * <p>Steel stays out of reach by hand with no rule about hands anywhere: its hardness is 15, over
     * every swing below, so nothing lands -- philosophy 7's hard gate, a genuine impossibility.
     */
    public static final St HAND_SWING_BLOW_ST = new St(3f);

    /** Invented. A tap: one Fu on copper, the finishing blow, and the only kind lead forgives. */
    public static final St HAND_SWING_TAP_ST = new St(1f);

    /**
     * Invented. A heavy swing: reaches brass (4) and bronze (5), and overshoots copper by a plate's
     * third in one blow. The playtest asked for a harder swing (2026-10-15).
     */
    public static final St HAND_SWING_HEAVY_ST = new St(5f);

    /** Invented. A swing with the Strength effect on: just over iron's hardness of 6, well under steel's. */
    public static final St HAND_SWING_MIGHTY_ST = new St(7f);

    /**
     * Invented. Food exhaustion per St of a hand swing; vanilla takes 4 exhaustion per hunger point
     * and 0.1 per melee attack. A heavy swing (5 St) costs 0.25, so sixteen of them are one point --
     * a working arm gets hungry, and harder work gets hungrier.
     */
    public static final float HAND_SWING_EXHAUSTION_PER_ST = 0.05f;

    /**
     * Placeholder. The share of the seated head's St ceiling a blow carries at each crank setting, before
     * gearing. The head sets the ceiling ({@link #HEAD_ST_PER_MU_COMPETENCE}); the setting picks within it, so
     * both stay meaningful. Half and an eighth keep the old 12 / 3 St against the old 24 St ceiling.
     */
    public static final float HAMMER_THROW_SHARE_SHORT = 0.5f;
    public static final float HAMMER_THROW_SHARE_LONG = 0.125f;

    /**
     * Placeholder. St a machine head can land per Mu of head per unit of its edge competence (the square root of
     * hardness times strength, less what brittleness costs it): St of a blow is the head's, never the
     * machine's. An iron head of four ingots (about 3.9 Mu, competence about 6.9) comes to 24 St, the ceiling
     * the Hammer and the Press had before heads existed.
     */
    public static final float HEAD_ST_PER_MU_COMPETENCE = 0.88f;

    /**
     * Placeholder. The most St of an ordinary blow ({@code HandSwing.BLOW}) a hand tool's head can carry, per Mu of
     * tool times the head's competence. The arm sets the blow and the head only has to carry it
     * ({@code ModularHandTool}: the head's figure is a share of the ordinary blow, capped at 1), so a better head
     * never lifts a hand over the hard gate steel stands behind. Fitted so a cobblestone head of four cells on a
     * stick (about 1.35 Mu, competence 5.5) comes to 3.75 St, a quarter over the 3 an ordinary blow asks, and a light
     * or soft head (bone, copper) falls short of it and lands a smaller share of every swing.
     */
    public static final float HAND_ST_PER_MU_COMPETENCE = 0.5f;

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
     * Philosophy 7: a hard gate is a physical impossibility, and a machine refusing to run is one
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

    /**
     * Bottoms of the condition bands, descending -- how battered a machine looks, in words.
     *
     * <h3>Why this is free, and has no instrument behind it</h3>
     * Damage to a machine is visible in a way its temperature is not. A mushroomed hammer head is
     * something you can see from across the room, so charging an instrument for it would be
     * hiding a free sense (§8). What the adjective withholds is the figure -- calipers exist for
     * that -- and the top band is silent entirely, so a sound machine adds no line to the HUD.
     */
    public static final float CONDITION_SOUND = 0.98f;
    public static final float CONDITION_MARKED = 0.85f;
    public static final float CONDITION_BATTERED = 0.65f;

    /**
     * Which condition band a figure falls in -- 0 sound, rising to 3 spent.
     * <p>
     * Lives here rather than in the readout because the server needs it too: wear happens blow by
     * blow and syncing every one of them would be a packet a tick for a line that changes four
     * times in the life of the machine. The server syncs when the <em>band</em> moves.
     */
    public static int conditionBand(float condition) {
        if (condition >= CONDITION_SOUND)
            return 0;
        if (condition >= CONDITION_MARKED)
            return 1;
        if (condition >= CONDITION_BATTERED)
            return 2;
        return 3;
    }

    // Work per blow is deliberately NOT a figure here. It is force divided by the material's
    // hardness -- see Deformation. A machine states the force it can deliver; what that
    // accomplishes is the material's business.

    /**
     * Below this, a coasting network is called stopped. Without a floor, speed approaches zero
     * asymptotically and machines tick forever at 0.0001 RPM.
     */
    public static final Rpm STOPPED_RPM_THRESHOLD = new Rpm(0.05f);

    // --- continuous deformation (machine shop) -----------------------------------------------

    /**
     * Invented. The Wire Drawer's two draw speeds, in St -- the same shape as the hand crank's
     * short/long throw (§17 of the philosophy), just relabelled for a machine with no crank of
     * its own: slow is gentle and always safe, fast clears a higher hardness at the cost of a
     * per-tick chance the wire snaps outright (see {@link #WIRE_SNAP_CHANCE}). Fast clears steel's
     * hardness of 15; slow does not, which is a genuine hard gate rather than a slower route --
     * steel wire is only reachable by accepting the risk, not by waiting it out.
     */
    public static final St WIRE_DRAWER_ST_SLOW = new St(6f);
    public static final St WIRE_DRAWER_ST_FAST = new St(18f);

    /** Invented. Chance per landed fast draw that the wire snaps outright (a draw lands about
     * four times a second at 32 RPM, so this is roughly the old 1%-per-tick rate). A
     * genuine loss -- the workpiece is destroyed, not merely wasted a tick's progress, per §7's
     * "failed batches must consume their inputs" applied to a deliberate risk rather than to
     * ignorance. */
    public static final float WIRE_SNAP_CHANCE = 0.05f;

    public static final Su WIRE_DRAWER_LOAD_SU = new Su(30f);

    /** Invented. The Rolling Mill has one setting -- no discrete choice is named for it in the
     * spec, so none is invented here. High enough to clear steel's hardness of 15 as well as
     * copper's. */
    public static final St ROLLING_MILL_ST = new St(20f);
    public static final Su ROLLING_MILL_LOAD_SU = new Su(50f);

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
     * the world -- philosophy §7's "failed batches must consume their inputs," read the other way
     * round: the removed mass is real material, not deleted, so a cut is allowed to pay some of
     * itself back. Deliberately probabilistic rather than "one swarf per Fu removed": the exact
     * accounting is a tuning question the spec explicitly defers, and this is the cheapest honest
     * version of "removal produces recoverable scrap."
     */
    public static final float SWARF_DROP_CHANCE = 0.15f;

    /** Invented. The Drill Press's stock bit -- a fixed hardness rating rather than a swappable
     * item this pass, since the Lathe (next) is where a physical, wearable tool bit actually
     * earns its own class. High enough to clear steel's hardness of 15. */
    public static final St DRILL_BIT_HARDNESS = new St(18f);

    /** Invented. The Drill Press's two feed speeds -- slow is always safe, fast risks a bound,
     * snapped bit (see {@link #BIT_SNAP_CHANCE}) but cuts faster. Both clear steel's hardness once
     * the bit itself does ({@link #DRILL_BIT_HARDNESS}); the choice is speed against risk, not
     * possible against impossible, unlike the Wire Drawer's pair. */
    public static final St DRILL_PRESS_ST_SLOW = new St(10f);
    public static final St DRILL_PRESS_ST_FAST = new St(22f);

    /** Invented. Chance per fast-feed push that the bit binds -- a wasted push and a warning
     * sound, not damage to the machine body or the workpiece (§7: a hard gate on the fast setting
     * alone would be a lock; this is a genuine risk instead). */
    public static final float BIT_SNAP_CHANCE = 0.05f;

    public static final Su DRILL_PRESS_LOAD_SU = new Su(35f);

    /**
     * Invented. The Lathe's tool bit -- a real physical component this time (see {@code
     * FDataComponents#TOOL_CONDITION}), not a fixed constant like the Drill Press's. A little
     * harder than the Drill Press's stock bit, since turning against a target diameter is the
     * finer of the two jobs.
     */
    public static final St LATHE_BIT_HARDNESS = new St(20f);

    /** Invented. Light/heavy pass -- same shape as every other discrete throw in the mod: heavy
     * cuts faster and coarser, light is slower and controllable. Neither risks the bit; only
     * cutting a material the bit's own rating cannot clear wears it (see {@link #LATHE_BIT_WEAR_PER_ST}). */
    public static final St LATHE_ST_LIGHT = new St(8f);
    public static final St LATHE_ST_HEAVY = new St(18f);
    public static final Su LATHE_LOAD_SU = new Su(40f);

    /** Invented. Same "force that cannot go into the work goes into the machine" rule the Hammer
     * already uses, applied to the bit instead of a machine body: a cut the bit's own rating
     * cannot clear wears it, landing a cut never does. */
    public static final float LATHE_BIT_WEAR_PER_ST = 0.0005f;
    public static final float LATHE_BIT_CONDITION_FLOOR = 0.4f;

    /**
     * Invented. The Grinding Wheel's own hardness rating -- set above every other removal tool in
     * this document ({@link #DRILL_BIT_HARDNESS}, {@link #LATHE_BIT_HARDNESS}) on purpose, so it
     * is the one route through a material like hardened tool steel rather than a finishing
     * upgrade. {@link io.github.soundgoodizerfan.feedback.process.Removal#canBeCutBy} is the same gate either way; this is simply the one
     * tool whose rating clears it.
     */
    public static final St GRINDING_WHEEL_HARDNESS = new St(30f);

    /** Invented. Coarse/fine grit -- coarse removes faster and wears the wheel faster; fine is
     * slower on both counts. Discrete, per §3, not a continuous grit number. */
    public static final St GRINDING_WHEEL_ST_COARSE = new St(16f);
    public static final St GRINDING_WHEEL_ST_FINE = new St(6f);
    public static final Su GRINDING_WHEEL_LOAD_SU = new Su(45f);

    /**
     * Invented. Condition lost per Fu ground, under *correct* use -- the stated exception to the
     * Hammer's "wear only from misuse" rule. A real grinding wheel sheds grit whether or not the
     * cut was a mistake, so this fires on every landed grind, not on a failed one -- see {@code
     * GrindingWheelBlockEntity}'s own class doc for why this diverges from the Lathe and the
     * Hammer. Coarse wastes more grit per Fu than fine, matching real abrasive wear.
     */
    public static final float GRINDING_WHEEL_WEAR_PER_FU_COARSE = 0.004f;
    public static final float GRINDING_WHEEL_WEAR_PER_FU_FINE = 0.0015f;

    /**
     * Invented. Unlike every other condition floor in the mod ({@link #HAMMER_CONDITION_FLOOR},
     * {@link #LATHE_BIT_CONDITION_FLOOR}), this one is zero: the wheel is genuinely consumed by
     * use, not merely battered. At zero its effective hardness ({@code condition x rating}) is
     * also zero, so a bald wheel fails {@link io.github.soundgoodizerfan.feedback.process.Removal#canBeCutBy} against anything -- a real hard
     * gate (§7), not a machine refusing to run for no physical reason. The block itself is not
     * destroyed; what is gone is the grit, and the block stays exactly as inspectable as any other
     * spent tool.
     */
    public static final float GRINDING_WHEEL_CONDITION_FLOOR = 0f;

    /** Invented. Tu added to the workpiece per Fu ground, proportional and small -- real
     * interface heat, not a process. Lets overrun grinding a hardened part re-soften it by
     * pushing it back into a tempering-relevant range, which needs no new mechanic: {@code
     * ItemHeat} already treats every workpiece this way regardless of how it got hot. */
    public static final float GRINDING_HEAT_PER_FU = 0.4f;

    // --- pulley and belt (machine shop) ------------------------------------------------------

    /**
     * Invented. Su a belt can transmit before it slips. Above this, the driven side receives
     * less speed than the nominal diameter ratio would give -- a genuinely different failure
     * character from a Shaft's overstress, which reads as zero speed across the whole network at
     * once. A belt sheds load instead: the far side simply turns more slowly, and the belt itself
     * wears for it (see {@link #BELT_WEAR_PER_SU_SLIPPED}).
     */
    public static final Su BELT_CAPACITY_SU = new Su(60f);

    /** Invented. Condition lost per Su of shortfall the belt slips through, per tick -- real wear
     * under real use, the same shape the Grinding Wheel's grit loss already established, because a
     * slipping belt genuinely burns itself on the pulley face. */
    public static final float BELT_WEAR_PER_SU_SLIPPED = 0.0008f;
    public static final float BELT_CONDITION_FLOOR = 0.3f;

    /** Invented. Longest belt, in blocks between pulley centres. A placement decision (§13's
     * argument for data-link range), not a realism figure. */
    public static final int BELT_MAX_LENGTH = 16;

    public static final Drag PULLEY_DRAG_SU_PER_RPM = new Drag(0.1f);
    public static final Inertia PULLEY_INERTIA = new Inertia(3f);

    // --- thermal ---------------------------------------------------------------------------

    /**
     * Invented. The temperature everything decays towards, in Tu -- <em>by default</em>.
     *
     * <h3>A fallback, not the figure</h3>
     * The room a body sits in is {@code AmbientTable.at(level)}: a datapack table keyed by
     * dimension, so a Nether foundry sits in warmer air than an overworld one. This is what an
     * unlisted dimension reads, what a block entity not yet in a level starts at, and the floor of
     * the food-suitability ramp, which has no level to ask. Biomes are deliberately not a key --
     * nobody moves a factory two degrees; a dimension is a decision.
     */
    public static final Tu AMBIENT_TU = new Tu(20f);

    /**
     * How fast a loose workpiece sheds heat, in Tu per tick, cooling in a straight line.
     *
     * <h3>What this number actually sets</h3>
     * It sets the size of the player's workshop. A steel ingot leaves the crucible around 1450 Tu
     * and hot working stops below 900 Tu, so the walk from fire to anvil is
     * {@code 550 / rate} ticks -- about fourteen seconds here. Halve the rate and placement stops
     * mattering; double it and the hammer has to be bolted to the crucible.
     * <p>
     * Straight-line rather than exponential, which was the first version. See
     * {@link io.github.soundgoodizerfan.feedback.core.thermal.Heat#cooled} for the argument -- briefly, an
     * exponential never arrives, needs an arbitrary floor to stop it, and hides the whole working
     * window in a flat tail. A constant rate makes "you have fourteen seconds" literally true,
     * which is what the player is budgeting against.
     * <p>
     * Deliberately one figure for every item rather than per-material. Specific heat is real and
     * TerraFirmaCraft gives every item its own; the test here is whether it creates a choice, and
     * with one hot material in the slice it does not. It becomes worth having the moment a player
     * must decide <em>which</em> of two hot things to carry first.
     */
    public static final TuRate ITEM_COOLING_TU_PER_TICK = new TuRate(1.5f);

    /**
     * Invented. Below this, a workpiece is close enough to ambient to stop being described as hot,
     * and its heat components are dropped.
     *
     * <h3>Why a threshold survived the switch to linear cooling</h3>
     * It is no longer load-bearing -- straight-line cooling reaches ambient exactly, so nothing
     * would linger forever. It stays because components are part of a stack's identity: an ingot
     * at 21 Tu and one at exactly 20 Tu are different items and will not merge, so the player's
     * chest fills with piles of one. Clearing slightly early makes a cooled ingot byte-identical
     * to one that was never heated.
     */
    public static final Tu WARM_TU = new Tu(60f);

    /**
     * Invented. How readily heat crosses from a fire into the vessel above it, in Work per tick
     * per Tu of difference.
     *
     * <h3>Why heat flows on a difference rather than at a flat rate</h3>
     * A flat {@code Work/t} was the first thing tried and it is wrong in a way that matters: with
     * it, a vessel's final temperature is {@code fire output / leak}, so insulating a crucible
     * raises the temperature it settles at <em>without limit</em>. Insulation became a trap
     * rather than an upgrade, which contradicts the slice.
     * <p>
     * Driving the flow on {@code (fire - vessel)} fixes it the way the real relationship does: a
     * vessel approaches its fire's temperature and can never pass it. Insulation then does
     * exactly what it should -- it gets you closer to the flame and holds you there, and the only
     * way past the flame is a hotter flame.
     * <p>
     * It also pays for the slice's lava for free. Lava is a fire fixed at 1200 Tu, so a crucible
     * over lava sits just under 1200 Tu forever: perfectly stable, and permanently too cool for
     * steel. No rule had to be written for that.
     */
    public static final Conductance FIRE_CONDUCTANCE = new Conductance(0.6f);

    /**
     * Invented. How fast a vessel bleeds heat to the room, in Work per tick per Tu above ambient.
     * One figure for every vessel: what differs between a small crucible and a large one is mass,
     * not surface.
     */
    public static final Conductance VESSEL_LEAK = new Conductance(0.025f);

    /**
     * Invented. Work required to raise a vessel by one Tu -- its thermal mass.
     *
     * <h3>The two figures are the whole of "upgrades are physical"</h3>
     * Mass sets the time constant, {@code mass / (conductance + leak)}: 40 ticks for the small
     * crucible and 400 for the large. Everything the slice claims about the two follows from
     * that one number and nothing else had to be written:
     * <ul>
     *   <li>The small one reaches its fire's temperature in about two seconds. Inside steel's
     *       window on a full fire it is still climbing at <b>7.9 Tu/t</b>, past carburizing's
     *       5 Tu/t limit, so every stroke of the bellows resets its hold and it cannot carburize
     *       on a full draught at all. The rate limit is not a balance rule bolted on; it is what
     *       makes a bigger vessel necessary. Note what it is <em>not</em>: a lock. A small
     *       crucible on a carefully geared, gentle draught still works, which is the standing rule
     *       that precision never gates hard (§7).</li>
     *   <li>The large one climbs at <b>0.79 Tu/t</b> there, so it cannot breach the limit however
     *       it is fired, and a 20-tick-old thermometer reading is still worth acting on -- it will
     *       have moved 16 Tu, comfortably inside steel's 60 Tu window. The same loop on the small
     *       crucible swings 159 Tu and peaks at 1599, <b>past the 1540 Tu at which iron burns</b>.
     *       That is the slice's thermostat bug, and it is a burnt batch rather than a wobble.</li>
     * </ul>
     * Neither is better. A process that needs to <em>move</em> would much rather have the small
     * one.
     */
    public static final ThermalMass CRUCIBLE_SMALL_MASS = new ThermalMass(25f);
    public static final ThermalMass CRUCIBLE_LARGE_MASS = new ThermalMass(250f);

    /**
     * Invented. The Hearth's brick pan: between the small and the large crucible, so it is slow
     * enough to hold a fire's heat through a gust of cold stock and quick enough to be warm by
     * the time the first part is shaped.
     */
    public static final ThermalMass HEARTH_MASS = new ThermalMass(60f);
    /**
     * Invented. An open pan loses heat from every face, so more than a vessel's {@link #VESSEL_LEAK}.
     * Against {@link #FIRE_CONDUCTANCE} (0.6) it settles at about nine tenths of its fire.
     */
    public static final Conductance HEARTH_LEAK = new Conductance(0.06f);
    /**
     * Invented. The share of the gap between the Hearth and a piece of stock beside it that closes
     * each tick: heat flows on the difference, so stock approaches the pan and never passes it.
     * Item cooling is {@link #ITEM_COOLING_TU_PER_TICK} (1.5 Tu/t), so stock beside a pan settles
     * 1.5 / 0.03 = 50 Tu under it.
     */
    public static final float HEARTH_STOCK_GRIP = 0.03f;

    /** Invented. How many items a crucible holds. Two, because carburizing needs iron and carbon. */
    public static final int CRUCIBLE_SLOTS = 2;

    /**
     * Invented. Capacity of the internal tank every crucible and thermal vessel gets once a
     * {@code ThermalProcess} can melt something into a fluid, in mB.
     * <p>
     * Not yet a unit (§17's {@code mB} has no {@code core/unit} type -- see TODO's own open item
     * on retyping the datapack records), so this stays a plain int the same way {@code Fuel}'s
     * duration does.
     */
    public static final int VESSEL_TANK_CAPACITY_MB = 1296;
    /** Invented. Hand distillation in a vessel: the share of a cut's mB that boils off each tick, and Work a Tu of the vessel and its charge holds. */
    public static final float VESSEL_BOIL_PER_TICK = 0.02f, VESSEL_BOIL_MASS_WORK_PER_TU = 40f;

    /**
     * What one mold holds -- one ingot's worth. Not invented: 144 mB/ingot (so 16 mB/nugget, 9
     * nuggets or 1296 mB to a block) is the de facto standard across the modded-Minecraft fluid
     * ecosystem, and matching it costs nothing and buys free interop with anything that already
     * assumes it.
     */
    public static final int MOLD_CAPACITY_MB = 144;

    /** Invented. How much a held item takes from a vessel's tank per dip, in mB -- a quarter of
     * {@link #MOLD_CAPACITY_MB}, because a coat is thinner than a cast. See {@code
     * process/Dip.java} and {@code machine/dip/Dipping.java}. */
    public static final int DIP_MB = 36;

    /**
     * Invented. What one block of insulation packed against a vessel multiplies its leak by, and
     * how many are counted.
     *
     * <h3>Why this is a block and not an upgrade slot</h3>
     * Philosophy 4: an upgrade is a physical component you could point at. Insulation is the most
     * literal case in the mod -- you build it around the thing, and how well it works depends on
     * how much of the thing you covered. Six faces, of which one is the fire and one the lid, so
     * four is the practical maximum and the cap is where the geometry already put it.
     */
    public static final float INSULATION_LEAK_FACTOR = 0.8f;
    public static final int INSULATION_MAX_BLOCKS = 4;

    /**
     * Invented. The thermal mass a Refractory Lining adds to the vessel it is fitted in, in
     * Work/Tu -- the first concrete {@code UpgradeFitting}.
     *
     * <h3>Why mass and not leak</h3>
     * The block above settles insulation: leak is something you build around a vessel, and a
     * second route to the same number as an item would be a duplicate answer, not a second one.
     * Mass is the other figure a vessel has, and a lining is literally more of the vessel -- a
     * thicker, denser wall. It is the same figure {@link #CRUCIBLE_SMALL_MASS} and
     * {@link #CRUCIBLE_LARGE_MASS} differ by, so it lands a small crucible partway to a large one.
     *
     * <h3>The trade, which is not a strict win</h3>
     * Mass moves nothing about where a vessel settles ({@code Heat.equilibrium} has no mass in
     * it), so it can never carry a vessel past its flame. It only stretches the time constant,
     * both ways: slower to climb (more fuel burnt getting there), slower to come back down, slower
     * to answer a thermostat. A batch that needs a rate limit held wants it; a job that needs to
     * <em>move</em> wants the bare small crucible. Doubles a small crucible and adds a tenth to a
     * large one, so it is the small crucible's way to buy steadiness without the large block.
     */
    public static final ThermalMass REFRACTORY_LINING_MASS = new ThermalMass(25f);

    /**
     * Invented. Extra leak each open Damper adds, additive on top of the insulated base -- a
     * vent bypasses the insulating layer rather than negating it, so it adds where insulation
     * multiplies. Same dimension as {@link #VESSEL_LEAK} by construction (Work/t/Tu).
     */
    public static final Conductance DAMPER_LEAK_BONUS = new Conductance(0.05f);
    public static final int DAMPER_MAX_BLOCKS = 4;

    // --- the boiler (thermal -> steam) -------------------------------------------------------

    /**
     * Invented. First bridge between two energy types (philosophy §10, §14): fire in, Steam out
     * -- and nothing else, deliberately, because the whole point of the split below is that this
     * block has no idea rotation exists.
     *
     * <h2>Two blocks, not one, and that is the design decision this section records</h2>
     * The first draft of this was one block, boiler and turbine in the same body, on the theory
     * that {@code ThermalVesselBlockEntity} already makes exactly that simplification over the
     * crucible-plus-firebox pair. That is wrong here in a way it is not there: a furnace is
     * always its own fire, but "what makes the heat" is precisely the axis philosophy §10 says
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
     *       pass; {@link #STEAM_ENGINE_CAPACITY_SU} stands in for a rotor that does not exist
     *       yet, the same way {@link #HAND_CRANK_CAPACITY_SU} once stood in for gearing.</li>
     *   <li><b>Not taken: EU as the output.</b> GTCEu's turbine produces electricity because
     *       everything in GTCEu eventually does. Feedback's point of departure (§10) is that
     *       Mechanical never has to become Electrical, so the engine's output is {@link Su}/
     *       {@link Rpm} on a plain {@code RotationNode}, full stop -- a flywheel and a belt, not
     *       a generator. Nothing stops a later Electrical-to-Thermal adapter feeding this same
     *       boiler, which is exactly the loop-back the class doc above names.</li>
     *   <li><b>Taken, later (2026-10-14): the exploding boiler.</b> `feedback_philosophy.md` §14
     *       names it as good precedent for a genuine hard-gate consequence. It was left out first
     *       because the boiler could not be over-restricted -- there was no failure state to
     *       explode into. It now has a pressure ({@link #BOILER_STEAM_MB_PER_PU},
     *       {@link #BOILER_PU_PER_TU_OVER}): a shut line under a real fire is the only way there,
     *       a weeping, scalding boiler gives warning first, and the ladder is the Pressure
     *       Vessel's own (`PressureBurst`). See `feedback_mechanics.md` §7.3.</li>
     * </ul>
     */
    public static final Tu BOILER_WORKING_TU = new Tu(120f);

    /**
     * Invented. Work required to raise the boiler by one Tu.
     * <p>
     * Between the two crucible masses ({@link #CRUCIBLE_SMALL_MASS}, {@link #CRUCIBLE_LARGE_MASS})
     * on purpose: a boiler's whole point is to be far easier to bring into its working band than
     * a smelting vessel is -- {@link #BOILER_WORKING_TU} sits below even {@link #FOOD_MAX_TU},
     * nowhere near {@link #METAL_MIN_TU} -- so it should climb like the small crucible, not the
     * large one.
     */
    public static final ThermalMass BOILER_MASS = new ThermalMass(40f);

    /**
     * Invented. How fast the boiler bleeds heat to the room when it is not making steam, in the
     * same {@code Work/t/Tu} every other leak is in. An ordinary vessel leak, {@link #VESSEL_LEAK}
     * -- the boiler is not an engine and has nothing analogous to an engine's exhaust; venting
     * heat as work happens explicitly, below, as mB of Steam, never through this figure.
     */
    public static final Conductance BOILER_LEAK = VESSEL_LEAK;

    /**
     * Invented. mB of Steam produced per tick once the boiler clears {@link #BOILER_WORKING_TU},
     * flat rather than scaled by how far past the threshold it sits -- the same "threshold, not a
     * curve" choice {@link #STEAM_ENGINE_CAPACITY_SU} makes on the consuming end, for the same
     * reason: a continuously variable rate is real future work once pressure is modelled, not
     * this pass's.
     * <p>
     * Consumes the same amount of water from the boiler's own tank -- 1 mB water to 1 mB steam,
     * which is wrong (real steam expands roughly 1600:1) and known to be wrong, the same way
     * TerraFirmaCraft's per-material heat capacity was known and declined for the same reason:
     * expansion ratio only matters once tank *size* is a real constraint the player is managing,
     * and right now {@link #BOILER_WATER_TANK_MB} and {@link #BOILER_STEAM_TANK_MB} are both
     * placeholders anyway. 1:1 is the honest way to say "this is a rate, not yet a volume."
     */
    public static final int BOILER_STEAM_PER_TICK_MB = 10;

    /**
     * Invented. Work drawn out of the boiler's own heat per mB of Steam made. Steam is where the
     * heat goes: without this a boiler at working temperature made steam for free, and output
     * did not depend on the fire at all. With it, steam output is capped by the heat flowing in
     * on the difference -- {@link #FIRE_CONDUCTANCE} x (fire - boiler) -- so a weak fire holds
     * the boiler at {@link #BOILER_WORKING_TU} making a trickle, and full output needs a fire
     * roughly 330 Tu hotter than that.
     */
    public static final float BOILER_WORK_PER_MB_STEAM = 20f;
    /**
     * Fluid temperature (mechanics 5.9). Invented. The share of the gap to its surroundings a tank's
     * contents close each tick (a tank, not a pipe: lines add no loss). A steam engine runs only on
     * steam at least {@code STEAM_ENGINE_MIN_TU} hot and sends it out {@code STEAM_ENGINE_EXHAUST_DROP_TU}
     * cooler (not below ambient), so a second engine can take the first's exhaust while it is still
     * above that line, and a condenser takes whatever is left.
     */
    public static final float FLUID_RELAX_PER_TICK = 0.002f;
    public static final float STEAM_ENGINE_MIN_TU = 110f, STEAM_ENGINE_EXHAUST_DROP_TU = 40f;
    /**
     * Placeholder (user ruling, 2026-10-23: output scales with the steam it gets). The engine's capacity is
     * {@link #STEAM_ENGINE_CAPACITY_SU} times band over {@link #STEAM_ENGINE_BANDS}, the band stepping up for every
     * {@code STEAM_ENGINE_BAND_TU} the steam is hotter than {@link #STEAM_ENGINE_MIN_TU}. Bands, not a curve, because
     * a source is only re-read on a rotation rebuild.
     */
    public static final int STEAM_ENGINE_BANDS = 4;
    public static final float STEAM_ENGINE_BAND_TU = 60f;

    /** Invented. Placeholder tank sizes -- see {@link #BOILER_STEAM_PER_TICK_MB}'s doc on why
     * the 1:1 ratio does not yet try to make these mean anything. */
    public static final int BOILER_WATER_TANK_MB = 4000;
    public static final int BOILER_STEAM_TANK_MB = 4000;

    // --- the steam engine (steam -> rotation) ------------------------------------------------

    /**
     * Invented. mB of Steam the engine draws per tick while it has that much buffered, and the
     * threshold for {@link #STEAM_ENGINE_CAPACITY_SU}/{@link #STEAM_ENGINE_RPM} switching on.
     * <p>
     * Matched to {@link #BOILER_STEAM_PER_TICK_MB} so one boiler at working temperature keeps
     * exactly one engine fed with nothing banked -- the number every future recipe/rebalance
     * pass is free to pull apart once there is a reason to want two boilers per engine or the
     * reverse.
     */
    public static final int STEAM_ENGINE_MB_PER_TICK = BOILER_STEAM_PER_TICK_MB;

    /** Invented. Placeholder input tank -- lets the engine coast a few ticks on a stutter in
     * supply rather than stalling the instant a boiler undershoots by one tick's worth. */
    public static final int STEAM_ENGINE_TANK_MB = 200;

    /**
     * Invented. What the engine supplies while it has {@link #STEAM_ENGINE_MB_PER_TICK} of Steam
     * to draw on, in Su -- flat, not scaled by how much is buffered.
     * <p>
     * Flat rather than continuous on purpose, the same choice the Water Wheel already makes with
     * its flowing-side count: {@link io.github.soundgoodizerfan.feedback.core.rotation.RotationNetwork}
     * only re-reads a source's numbers on an explicit rebuild, never every tick, so a source whose
     * output tracked buffered Steam continuously would need a rebuild every tick it moved -- the
     * one thing the whole propagator is built to never do (see its own class doc: a rebuild
     * visits every node once, which is only affordable because it does not happen constantly). A
     * genuinely continuous throttle is real future work -- GTCEu's turbine does exactly that --
     * but it wants the rotor-as-upgrade-item this section's other doc explicitly defers. Starved
     * of Steam: philosophy §7's genuine hard gate, not enough pressure, and not a precision
     * problem at all.
     */
    public static final Su STEAM_ENGINE_CAPACITY_SU = new Su(64f);

    /** Invented. Flat output speed once working -- see {@link #STEAM_ENGINE_CAPACITY_SU}'s doc
     * on why this is a threshold and not a curve. */
    public static final Rpm STEAM_ENGINE_RPM = new Rpm(16f);

    /**
     * Invented. A flywheel, and a real one -- heavier than a shaft or a cog, lighter than the
     * Water Wheel's slab of timber. See {@link #SHAFT_INERTIA}'s doc for how the inertia figures
     * are meant to be read against the Su figures they pair with.
     */
    public static final Inertia STEAM_ENGINE_INERTIA = new Inertia(150f);

    /**
     * Invented, placeholder. The speed a centrifugal overspeed trip lets go at: above the Hand
     * Crank's {@link #HAND_CRANK_RPM} and the engine's {@link #STEAM_ENGINE_RPM}, so a run turning
     * at its source's own speed never trips it and only a run geared up past that does.
     * <p>
     * One fixed figure and no setting, the bimetallic strip's rule ({@link #BIMETALLIC_TRIP_TU}):
     * flyweights and a spring are cut for one speed, and wanting a different one means a
     * different part, not a dial (philosophy 3).
     */
    public static final Rpm OVERSPEED_TRIP_RPM = new Rpm(48f);

    // --- vanilla vessels (§15) --------------------------------------------------------------

    /**
     * Invented. Above this, a food-type cooking recipe burns instead of finishing, in Tu.
     *
     * <h3>Why food and metal are told apart by recipe type, not a material table</h3>
     * The fallback (see {@link io.github.soundgoodizerfan.feedback.process.VanillaFallback})
     * exists so that no recipe in any installed mod is uncraftable, which rules out a
     * hand-authored temperature per material --
     * that is exactly the per-item table the fallback is there to avoid needing. Vanilla's own
     * {@code minecraft:smoking} recipe type already says "this is food"; reusing it is a
     * heuristic, not a physical measurement, and it is licensed by the same sentence that
     * licenses the rest of the fallback: a compatibility shim, not a model of the world.
     * <p>
     * Set below a plain furnace's charcoal equilibrium on purpose -- an unwatched furnace
     * burning its food is the mod's own core pitch (§1) landing on a block the player has
     * walked past since their first night. Nobody wrote that rule either.
     */
    public static final Tu FOOD_MAX_TU = new Tu(400f);

    /** Invented. Tu of crop weather per vanilla biome temperature unit above or below 0.8 (plains), added to the dimension's ambient (modular foods spec 2.3). */
    public static final float CROP_TU_PER_CLIMATE = 25f;

    /** Invented. Food starts to cook above this, in Tu: the pathogen starts dying (food spec 4.3). */
    public static final Tu FOOD_COOK_TU = new Tu(70f);

    /** Invented. Food reaches full cooking speed at this temperature; between the two it cooks proportionally slower. */
    public static final Tu FOOD_COOK_FULL_TU = new Tu(200f);

    /** Invented. Ticks of full-speed cooking to take a raw food to done. */
    public static final float FOOD_COOK_TICKS = 200f;

    /** Invented. Water leaves a food above this, in Tu, faster the hotter. */
    public static final Tu FOOD_EVAPORATION_TU = new Tu(100f);

    /** Invented. Fraction of moisture lost per tick, per Tu above {@link #FOOD_EVAPORATION_TU}. */
    public static final float FOOD_DRYING_PER_TU_TICK = 2.5e-6f;

    /** Invented. Where food burns when its material states no oxidation temperature, in Tu. */
    public static final Tu FOOD_CHAR_TU = new Tu(300f);

    /** Invented. Char gained per tick, per Tu above the food's char temperature; 1 is charred. */
    public static final float FOOD_CHAR_PER_TU_TICK = 1e-4f;

    /** Invented. Dried means below this fraction of the moisture the food started with. */
    public static final float FOOD_DRIED_FRACTION = 0.6f;

    /** Invented. Hunger and saturation of a dried food, against its done figure. */
    public static final float FOOD_DRIED_SCALE = 0.6f;

    /** Invented. Hunger and saturation of a charred food, against its done figure. */
    public static final float FOOD_CHARRED_SCALE = 0.1f;

    /** Invented. The skillet (food spec 6): ticks for the food's own heat to close most of the way (63%) to the pan's. A thick cut would lag longer; see {@code SkilletBlock#warm}. */
    public static final float SKILLET_LAG_TICKS = 60f;

    /** Invented. Ticks one sample of the skillet path stands for, and how many samples a path may hold (30 s). */
    public static final int SKILLET_SAMPLE_TICKS = 2;
    public static final int SKILLET_MAX_SAMPLES = 300;

    /** Invented. The stockpot (food spec 5): water boils here, in Tu, and holds the pot at it while any is left. */
    public static final Tu POT_BOIL_TU = new Tu(100f);

    /** Invented. How fast the pot's heat closes on its target, per tick. */
    public static final float POT_RELAX_PER_TICK = 0.02f;

    /** Invented. The empty pot's own thermal mass, as mB of water it weighs the same as: contents add to it, so a full pot warms slowly and cold water or food cools a hot one when added. */
    public static final float POT_EMPTY_MASS_MB = 600f;

    /** Invented. mB of water boiled off per tick, per Tu the fire is over {@link #POT_BOIL_TU}. */
    public static final float POT_EVAPORATION_MB_PER_TU_TICK = 0.00167f;

    /** Invented. mB one ingredient adds to the pot, the most water it takes, and the most ingredients. */
    public static final float POT_ITEM_MB = 250f;
    public static final float POT_MAX_WATER_MB = 2000f;
    public static final int POT_MAX_ITEMS = 6;

    /** Invented. mB of mixture to a serving. */
    public static final float POT_SERVING_MB = 400f;

    /** Invented. Hunger and saturation a stew gives against its ingredients eaten separately: nothing is left on the bone or in the pan. */
    public static final float POT_STEW_BONUS = 1.25f;

    /** Invented. The pot minigame (food spec 6): the fire heat over which an unstirred pot scorches on the bottom even with water in it, the char per tick per Tu over that (fully stirred it is none), how much stirring one tick of full pace adds to how mixed the pot is and how fast mixed falls off, the scum a raw load yields per tick, how much one skim lifts, how much of the scum a full-pace tick of stirring folds into the body, and what scum and folded scum add to a serving's pathogen. */
    public static final Tu POT_SCORCH_FIRE_TU = new Tu(250f);
    public static final float POT_SCORCH_PER_TU_TICK = 5e-6f;
    public static final float POT_MIX_PER_TICK = 0.05f;
    public static final float POT_MIX_DECAY_PER_TICK = 0.004f;
    public static final float POT_SCUM_YIELD = 0.015f;
    public static final float POT_SKIM_AMOUNT = 0.25f;
    public static final float POT_FOLD_PER_TICK = 0.02f;
    public static final float POT_SCUM_HAZARD = 0.4f;

    /** Invented. Share of solids in the mixture under which a pot is a broth, not a stew. */
    public static final float POT_BROTH_SOLIDS = 0.2f;

    /** Invented. Heat kills this fraction of a food's live pathogen per tick at full cooking speed. */
    public static final float FOOD_KILL_PER_TICK = 0.03f;

    /** Invented. Spoilage (food spec 8): ticks for a wet food to rot through at ambient (2 h), the pathogen rot raises it to, the rot past which it turns toxic, the toxin it reaches, the rot at which it reads as going off, and the share of hunger rot takes. */
    public static final float FOOD_ROT_TICKS = 144000f;
    public static final float FOOD_ROT_PATHOGEN = 0.6f;
    public static final float FOOD_ROT_TOXIC_AT = 0.7f;
    public static final float FOOD_ROT_TOXIN = 0.8f;
    public static final float FOOD_ROT_GOING_OFF = 0.3f;
    public static final float FOOD_ROT_NUTRITION_LOSS = 0.5f;

    /** Invented. Smoke (food spec 8): the coat a figure-1 smoker lays down per tick on food cooking in it, the coat lost per tick (about 17 min from full), and the share of the rot rate a full coat removes. */
    public static final float SMOKE_DEPOSIT_PER_TICK = 0.0006f;
    public static final float SMOKE_DECAY_PER_TICK = 1f / 20000f;
    public static final float SMOKE_ROT_SHIELD = 0.6f;
    /** Invented. The figure a smoker has: how much smoke it lays down against a figure-1 vessel. The furnace and blast furnace have none. */
    public static final float SMOKER_SMOKE = 1f;
    /** Invented. The smoke coat above which a tooltip says Smoked. */
    public static final float SMOKE_SHOWN_AT = 0.1f;

    /** Invented. Salt (food spec 8): the share of the rot rate full salting removes, how much more hazard a full salting tolerates, the moisture one salting draws out, and the mB of brine boiled dry per salt item (only by the sea). */
    public static final float SALT_ROT_SHIELD = 0.8f;
    public static final float SALT_TOLERANCE = 1.5f;
    public static final float SALT_DRAW = 0.15f;
    public static final float SALT_MB_PER_ITEM = 500f;

    /** Invented. A fresh food is stamped to the nearest minute, so stacks made close together still merge. */
    public static final long FOOD_STAMP_BUCKET = 1200L;

    /** Invented. The ice box: ticks one piece of ice keeps it cold, the temperature it holds while it has any, and the fraction of the way to the room it drifts per second once it has none. */
    public static final float ICEBOX_ICE_TICKS = 12000f;
    public static final Tu ICEBOX_COLD_TU = new Tu(2f);
    public static final float ICEBOX_DRIFT_PER_SECOND = 0.005f;

    /**
     * Invented. The Refrigeration Unit (food spec 8): amps it asks for (it has no thermostat, the
     * player's breaker is the switch), the ticks an Ice Box keeps trusting it after its last pulse,
     * the temperature its radiator is driven toward while running (it settles about 6/7 of the way
     * from the room, over the scald line), and its mass and leak (warms in about half a minute,
     * cools in a few minutes).
     */
    public static final float REFRIGERATION_AMPS = 2f;
    public static final int REFRIGERATION_LAPSE_TICKS = 30;
    public static final Tu REFRIGERATION_HOT_TU = new Tu(130f);
    public static final ThermalMass REFRIGERATION_MASS = new ThermalMass(400f);
    public static final Conductance REFRIGERATION_LEAK = new Conductance(0.1f);

    /** Invented. The drying rack: moisture lost per tick at ambient (it scales with the air's heat), the hottest the air may be before food would cook, and the most food it holds. */
    public static final float RACK_DRY_PER_TICK = 4.7e-5f;
    public static final Tu RACK_MAX_AIR_TU = new Tu(65f);
    public static final int RACK_SLOTS = 4;

    /** Invented. Buffs (food spec 7): ticks of delay per unit of fat plus fibre in a food, the least strength of a compound that does anything, the fat plus fibre above which a dose comes on too slowly to crash. */
    public static final float FOOD_BUFF_DELAY_TICKS = 600f;
    public static final float FOOD_BUFF_MIN_STRENGTH = 0.05f;
    public static final float FOOD_BUFF_CRASH_ABSORB = 0.3f;

    /** Invented. A dish on a warming container (blaze rod) is held at this heat, in Tu: warm, under the point a bare hand feels pain. */
    public static final Tu WARMING_HOLD_TU = new Tu(70f);

    /** Invented. A dish whose container has the featherlight perk (breeze rod) takes this share of the eating time. */
    public static final float DISH_FEATHER_EAT_SCALE = 0.6f;

    /** Invented. Eating a warm dish (above the warm point) comforts: ticks of Regeneration, and the cold it carries is shed. */
    public static final int WARM_MEAL_REGEN_TICKS = 80;

    /** Invented. Food-perk traits, per twice-a-second pass at strength 1: freeze ticks shed (powder snow adds 1 a tick, so 12 outruns it), fire ticks shed extra, air ticks added (10 is neutral). */
    public static final int PERK_FREEZE_RELIEF_TICKS = 12;
    public static final int PERK_COOL_BURN_TICKS = 30;
    public static final int PERK_AIR_TICKS = 10;
    /** Invented. Natural-regeneration perks work only at or above this food level, the vanilla fed line. */
    public static final int PERK_FED_FOOD = 18;

    /** Invented. A pathogen or toxin load under this does nothing when eaten. */
    public static final float FOOD_HAZARD_TOLERANCE = 0.05f;

    /**
     * Invented. A laid egg's pathogen follows how crowded the flock is: disease spreads in a pen. One bird alone (free range)
     * lays at the first figure, just over the hazard line, so a raw egg is usually fine and sometimes not; each further
     * chicken within the radius adds the second, up to the cap. Rot adds on top by the usual clock.
     */
    public static final float EGG_FREE_RANGE_PATHOGEN = 0.06f;
    public static final float EGG_CROWD_PATHOGEN = 0.05f;
    public static final float EGG_CROWD_PATHOGEN_MAX = 0.5f;
    public static final double EGG_CROWD_RADIUS = 4.0;

    /**
     * Invented. The adjectives a dish earns from its figures (dish spec 5): blended water over soggy or under dry, fat share
     * over rich, hunger per litre of container over dense or under light, the strongest compound's concentration over bright
     * or under bland. Each is glossed on Shift from the constant that decides it.
     */
    public static final float DISH_SOGGY_MOISTURE = 0.8f;
    public static final float DISH_DRY_MOISTURE = 0.45f;
    public static final float DISH_RICH_FAT = 0.25f;
    public static final float DISH_DENSE_HUNGER_PER_LITRE = 16f;
    public static final float DISH_LIGHT_HUNGER_PER_LITRE = 8f;
    public static final float DISH_BRIGHT_COMPOUND = 0.25f;
    public static final float DISH_BLAND_COMPOUND = 0.05f;

    /** Invented. A test strip reads a food's toxin in quarter steps: four bands, never a figure it cannot stand behind. */
    public static final float TEST_STRIP_RESOLUTION_TOXIN = 0.25f;

    /**
     * Invented. The bite (dish spec 5, the zero-tier instrument): a nibble applies the food's hazard effects with chance and
     * duration times this share, and can be taken again after the cooldown.
     */
    public static final float BITE_SHARE = 0.15f;
    /** Invented. A sneak-press released within this many ticks of the eating starting is a nibble, not a meal. */
    public static final int BITE_TAP_TICKS = 6;

    public static final int BITE_COOLDOWN_TICKS = 20;
    public static final int BITE_HUD_TICKS = 60;

    /** Invented. Dishes (dish spec 2.3): mB one food part adds to the dish's mixture (what toxin and compounds are diluted over), the hunger a litre of container holds, seconds a dish takes to eat, a base plus a share per litre of container (a skewer is a snack, a plate is not), and the knife's durability (a point per dish). */
    public static final float DISH_PART_MB = 150f;
    public static final float DISH_HUNGER_PER_LITRE = 20f;
    public static final float DISH_EAT_BASE_SECONDS = 0.6f;
    public static final float DISH_EAT_SECONDS_PER_LITRE = 3f;

    /** Invented. The water in a pot serving used as a soup's base (a ladle of broth or stew): wet, but not all water. */
    public static final float SERVING_MOISTURE = 0.9f;

    /**
     * Invented. Moisture migration (dish spec 2.3): across each neighbouring pair of food parts this share of the gap in water
     * moves from the wetter to the drier (conserved: the dish's blend does not change), times the container's seal scale. A
     * part under {@link #DISH_DRY_MOISTURE} next to one over {@link #DISH_SOGGY_MOISTURE} is soaked and feeds {@link #SOAK_SCALE}
     * of its hunger and saturation.
     */
    public static final float MIGRATION = 0.5f;
    public static final float SOAK_SCALE = 0.85f;

    /**
     * Invented. Seal and spill (dish spec 2.2): a container's seal 0 open, 1 covered, 2 sealed takes this much off the scale of
     * drying, migration and spill per level (covered halves them, sealed stops them), and sealed stops rot. An overfull dish
     * loses this share of its spillable overflow (mB over the container's, in parts of {@link #DISH_PART_MB}) from its wettest part each
     * time it is picked up or dropped, until it is down to what the container holds.
     */
    public static final float SEAL_SCALE_PER_LEVEL = 0.5f;
    public static final float SPILL_SHARE = 0.34f;

    /**
     * Invented. Prep Table work (dish spec 2.2, 4). A cut food (level 1 sliced, 2 minced) rots, cooks and dries, and gives up
     * its compounds, faster by these per level; a ragged cut leaves a thick middle that heat does not clear below the
     * pathogen floor; the evenness grades by the spread of piece lengths (coefficient of variation).
     */
    public static final float CUT_ROT_PER_LEVEL = 0.5f;
    public static final float CUT_HEAT_PER_LEVEL = 0.3f;
    public static final float CUT_FIBRE_LOSS_PER_LEVEL = 0.25f;
    public static final float CUT_RAGGED_PATHOGEN_FLOOR = 0.08f;
    public static final float CUT_EVEN_CV = 0.15f;
    public static final float CUT_MIXED_CV = 0.4f;
    /** Invented. The knife's feel: grain lines every this many line units, how hard fibre pulls a stroke to the nearest one (at fibre 1), and how far fat lets it slide on. */
    public static final int CUT_GRAIN = 125;
    public static final float CUT_GRAIN_PULL = 0.8f;
    public static final float CUT_FAT_SLIDE = 60f;
    public static final int CUT_MAX_MARKS = 6;
    public static final int CUT_MINCED_MARKS = 4;
    /** Invented. Ticks a mark may be placed no faster than (the server refuses a result sent quicker than that), and the slack for the begin packet's travel. */
    public static final int CUT_TICKS_PER_MARK = 4;
    public static final int PREP_SLACK_TICKS = 10;

    /**
     * Invented. The pour: a vessel fills to 1000 units and the line sits at {@link #POUR_TARGET}; a food of viscosity 1 (or
     * none given) pours {@link #POUR_FLOW} units a tick, and each decade of viscosity slows it by {@link #POUR_DRAG}
     * of that (honey, 10000, runs a quarter of the speed). Strength is poured over the line, kept between the floor and
     * ceiling; within {@link #POUR_CLEAN} of the line is clean, within {@link #POUR_SHAKY} shaky. A finish nobody poured
     * by hand counts as {@link #POUR_AUTOMATION} (sloppy, never a refusal). The hold is capped at {@link #POUR_MAX_TICKS}.
     */
    public static final int POUR_FULL = 1000, POUR_TARGET = 600;
    public static final float POUR_FLOW = 24f;
    public static final float POUR_DRAG = 0.75f;
    public static final float POUR_CLEAN = 0.10f;
    public static final float POUR_SHAKY = 0.30f;
    public static final float POUR_MIN = 0.3f, POUR_MAX = 2.0f;
    public static final float POUR_AUTOMATION = 0.8f;
    public static final int POUR_MAX_TICKS = 400;

    /**
     * Invented. The knead ({@code docs/feedback_food_spec.md} 6): one sample every {@link #KNEAD_STEP_TICKS} ticks of how hard
     * the dough is pressed, 0..100, at most {@link #KNEAD_MAX_SAMPLES} (the session length) and at least {@link #KNEAD_MIN_SAMPLES}.
     * Per sample at full press: gluten builds {@link #KNEAD_GLUTEN} (less the more water {@link #KNEAD_STICKY} and fat
     * {@link #KNEAD_FAT_GLUTEN} hold, and less the more tense the dough {@link #KNEAD_TENSE_RESIST}); tension builds
     * {@link #KNEAD_TENSION} by the square of the press (more with fibre {@link #KNEAD_FIBRE_TENSION}, less with fat
     * {@link #KNEAD_FAT_TENSION}) and rests away {@link #KNEAD_REST} by the slack of the press (slower with fibre
     * {@link #KNEAD_FIBRE_REST}). Tension past the tear limit ({@link #KNEAD_TEAR} scaled by {@link #KNEAD_TEAR_DRY} to
     * {@link #KNEAD_TEAR_WET} with moisture, so dry dough tears sooner) piles up overwork; {@link #KNEAD_TOUGH_OVER} of it is
     * tough. Otherwise gluten under {@link #KNEAD_GOOD_AT} is crumbly, over {@link #KNEAD_GLOSSY_AT} with the dough rested
     * back under {@link #KNEAD_RELAXED} is glossy, else good. The baked loaf's hunger and saturation scale by class; dough
     * nobody kneaded bakes at {@link #KNEAD_UNKNEADED_SCALE}, the unskilled floor.
     */
    public static final int KNEAD_STEP_TICKS = 2, KNEAD_MAX_SAMPLES = 300, KNEAD_MIN_SAMPLES = 10;
    public static final float KNEAD_GLUTEN = 0.02f, KNEAD_STICKY = 0.8f, KNEAD_FAT_GLUTEN = 0.5f, KNEAD_TENSE_RESIST = 0.6f;
    public static final float KNEAD_TENSION = 0.024f, KNEAD_FIBRE_TENSION = 1.0f, KNEAD_FAT_TENSION = 0.5f;
    public static final float KNEAD_REST = 0.03f, KNEAD_FIBRE_REST = 0.8f;
    public static final float KNEAD_TEAR = 1.0f, KNEAD_TEAR_DRY = 0.6f, KNEAD_TEAR_WET = 1.4f, KNEAD_TOUGH_OVER = 0.5f;
    public static final float KNEAD_GOOD_AT = 0.5f, KNEAD_GLOSSY_AT = 0.85f, KNEAD_RELAXED = 0.35f;
    public static final float KNEAD_GLOSSY_SCALE = 1.15f, KNEAD_GOOD_SCALE = 1.0f, KNEAD_CRUMBLY_SCALE = 0.8f, KNEAD_TOUGH_SCALE = 0.7f;
    public static final float KNEAD_UNKNEADED_SCALE = 0.9f;
    /** Invented. The share of the samples' ticks the server insists have passed (the rest is allowance for a lagging server), so a knead cannot be sent faster than it could be played. */
    public static final float KNEAD_TIME_SHARE = 0.8f;

    /**
     * Invented. Below this, a metal-type (smelting or blasting) cooking recipe does not progress
     * at all, in Tu.
     *
     * <h3>What this actually gates</h3>
     * A plain furnace fed only {@code logs} (760 Tu, see the {@code fuel} table) settles under
     * this line and simply cannot smelt ore -- fed charcoal or coal it clears it easily. That was
     * not tuned to be a lesson; it fell out of picking one number for each side and is worth
     * keeping regardless.
     */
    public static final Tu METAL_MIN_TU = new Tu(800f);

    /**
     * Invented. Hard ceiling on a Smoker's own temperature, in Tu -- the one explicit band wall
     * in §15, because leak and mass alone do not produce one.
     *
     * <h3>Below {@link #FOOD_MAX_TU}, not above it -- got backwards once already</h3>
     * The first version of this wall sat at 600: below {@link #METAL_MIN_TU} (800), and — the
     * reasoning actually written down here at the time — "comfortably above {@code
     * FOOD_MAX_TU}." That was exactly backwards, and it shipped a Smoker that burned its own food
     * just as fast as a plain Furnace. The mistake was treating "cannot reach metal's floor" as
     * the whole of what a *food* appliance needs, when the real requirement is the mirror image:
     * it must never reach food's ceiling either. {@link #FIRE_CONDUCTANCE} (0.6) against this
     * mass makes a lit vessel close most of the gap to its fire's temperature in well under a
     * second -- far faster than any food recipe's own cook time -- so a wall merely *below*
     * metal's floor still leaves the whole 400-800 Tu gap for the climb to blow straight through
     * on the way. The wall has to sit under {@link #FOOD_MAX_TU} instead, so the vessel settles
     * there and holds, rather than crossing it in transit.
     *
     * <h3>Why this is a clamp and the Furnace gets none</h3>
     * {@link #FIRE_CONDUCTANCE} dominates any leak this file could reasonably pick, so every
     * vessel's equilibrium sits close to its fire's own temperature almost regardless of leak --
     * mass changes how fast a vessel gets there and how much fuel noise it smooths out, not how
     * hot it tops out. A Smoker specialised by leak alone would still smelt ore given a hot
     * enough fuel, which is a whitelist by another name and exactly what §15 is written against.
     * So it gets a real physical wall instead. The Furnace and the Crude Blast Furnace get no
     * such clamp -- their whole difference from each other and from this is mass and leak, which
     * is the genuinely emergent half of the claim. The Smoker is the one place the mechanism is
     * declared rather than discovered, and this should say so rather than pretend otherwise.
     *
     * <h3>Under {@link #FOOD_CHAR_TU}, over {@link #FOOD_COOK_FULL_TU} -- got wrong a second time</h3>
     * 380 cleared {@link #FOOD_MAX_TU}'s old reasoning but not the food rework's: food chars above
     * 300, so a Smoker at its ceiling burned meat in about six seconds, before the pathogen was
     * dead. The wall now sits between full cooking speed (200) and char (300): the Smoker cooks
     * at full rate and cannot char food however long it is left. Food in it is still perishable
     * to continued operation -- it dries out (evaporation starts at 100) -- just never to char.
     */
    public static final Tu SMOKER_CEILING_TU = new Tu(250f);

    /**
     * Invented. Furnace thermal mass and leak -- the generalist, mediocre at both bands.
     *
     * <h3>No ceiling, on purpose</h3>
     * Unlike the Smoker, the Furnace's mediocrity is not a wall; it is <em>unreliability</em>.
     * Low mass means it tracks its fire's own noise almost directly, so a smelt in progress can
     * dip under {@link #METAL_MIN_TU} mid-hold on a rough batch of fuel and stall rather than
     * fail outright (§6) -- and the same low mass clears {@link #FOOD_MAX_TU} quickly if nobody
     * comes back, so an unwatched furnace burns its food. Both are real behaviour under one pair
     * of numbers, not two rules.
     */
    public static final ThermalMass FURNACE_MASS = new ThermalMass(20f);
    public static final Conductance FURNACE_LEAK = new Conductance(0.05f);

    /**
     * Invented. Smoker thermal mass and leak -- as low as the Furnace's, deliberately. Its
     * specialisation is entirely {@link #SMOKER_CEILING_TU}; these two numbers only make it
     * heat up fast and cheaply, which is the other half of "twitchy" (§15).
     */
    public static final ThermalMass SMOKER_MASS = new ThermalMass(15f);
    public static final Conductance SMOKER_LEAK = new Conductance(0.05f);

    /**
     * Invented. Crude Blast Furnace thermal mass and leak -- higher mass than either of our own
     * crucibles, deliberately. It is a sealed refractory box built for exactly one job, and §15
     * calls it "genuinely stable for a primitive device" -- a claim this file should not make
     * cheaply by giving it a worse leak than the Furnace and calling it a day.
     *
     * <h3>The floor is not a third number</h3>
     * §15 describes the Crude Blast Furnace as having its own high floor, worded as if it were a
     * wall the way the Smoker's ceiling is. It is not modelled as one here -- that would mean a
     * real bistable "roaring or out" combustion state, which is simulation the mod does not need
     * (does modelling it create a choice the player does not already have from watching the
     * climb?). What high mass actually buys is <em>time</em>: the climb through the low-Tu zone,
     * where it would ruin food or fail to smelt, is short relative to how long it then holds
     * {@link #METAL_MIN_TU} and above once it gets there -- the same claim in practice, with no
     * state machine underneath it. Recorded here so it is not mistaken for an oversight and
     * re-added as a third number later.
     */
    public static final ThermalMass BLAST_FURNACE_MASS = new ThermalMass(300f);
    public static final Conductance BLAST_FURNACE_LEAK = new Conductance(0.01f);

    /**
     * Invented. Improved Furnace thermal mass and leak -- three times the plain Furnace's mass,
     * half its leak: steadier to hold, slower to lose what it has, per the user's own framing
     * ("more stable... less loss"). No ceiling, same as the plain Furnace -- it stays the
     * generalist, just a better-built one, rather than becoming a fourth specialist.
     *
     * <h3>Where the thermowell actually went, and why it moved rather than doubled up</h3>
     * {@link VesselKind#FURNACE} carried {@code hasThermowell = true} before this vessel existed,
     * unused -- nothing implemented {@code Fittable} on {@code ThermalVesselBlockEntity} yet, so
     * it was a forward-looking guess with nothing to check it. Now that {@link
     * io.github.soundgoodizerfan.feedback.fitting.Fittable} is wired in, this is the vessel that
     * should actually carry the flag: the plain Furnace stays cheap, twitchy, and appliance-only
     * (§15), which is its whole identity, while this one is the appliance-family vessel that
     * finally earns a Sensor Fitting the way the Crucible always could. Moving the flag rather
     * than setting it on both keeps exactly one vessel meaning "designed to be measured" per era
     * -- the Crucible early, this one once steel exists.
     *
     * <h3>What this does not do, so it does not swallow the Crucible's whole niche</h3>
     * It cannot alloy -- {@code AlloyMix} is Crucible-only, not wired onto {@code
     * ThermalVesselBlockEntity} here or elsewhere -- and it cannot take insulation or a Damper,
     * both scoped to the Crucible family on purpose (see {@code DamperBlock}'s own doc). It is a
     * steadier, observable single-material appliance; the Crucible remains the only vessel that
     * is also a <em>component</em> a player can build further onto (§15's whole distinction).
     */
    public static final ThermalMass IMPROVED_FURNACE_MASS = new ThermalMass(60f);
    public static final Conductance IMPROVED_FURNACE_LEAK = new Conductance(0.025f);

    /**
     * Invented. Kiln thermal mass and leak -- between the Furnace's and the Smoker's, so a firing
     * hold is realistic without needing the Blast Furnace's minutes-long crawl. {@link
     * #KILN_CEILING_TU} does the actual specialising, per the Smoker's own precedent (§15's "one
     * wall, not two"): mass and leak only decide how fast it gets there and how much it wobbles.
     */
    public static final ThermalMass KILN_MASS = new ThermalMass(30f);
    public static final Conductance KILN_LEAK = new Conductance(0.04f);

    /**
     * Invented. Hard ceiling on a Kiln's own temperature, in Tu -- the Smoker's wall, reused for
     * the opposite band. {@code firebrick.json}'s window tops out at 1500 with its optimum at
     * 1300; {@link #METAL_MIN_TU} (800) is nowhere near it, so the wall that actually matters here
     * is steel's own floor, {@code steel_ingot.json}'s 1350. Set a little under that -- a Kiln can
     * fire ceramic and firebrick both, comfortably, and can never reach a steelmaking temperature
     * no matter what is shovelled into it, which is a genuine hard gate (§7) rather than a
     * whitelist: nothing anywhere checks that a Kiln is not a smelter.
     */
    public static final Tu KILN_CEILING_TU = new Tu(1330f);

    /**
     * Invented. Annealing Furnace thermal mass and leak -- higher than the Furnace's, lower than
     * the Blast Furnace's. The whole point of this vessel is a slow, even coast back down once its
     * one loaded charge of fuel runs out, which is mass buying time exactly the way it does for
     * the Blast Furnace (§15) -- just at a tenth of the operating temperature, so it does not also
     * need the Blast Furnace's near-total sealing to get there in a reasonable span.
     */
    public static final ThermalMass ANNEALING_FURNACE_MASS = new ThermalMass(150f);
    public static final Conductance ANNEALING_FURNACE_LEAK = new Conductance(0.02f);

    /**
     * Invented. Hard ceiling on an Annealing Furnace's own temperature, in Tu -- the Smoker's wall
     * a third time. {@code tempered_steel.json} wants 300-400, so this sits where {@link
     * #SMOKER_CEILING_TU} sits under {@link #FOOD_MAX_TU}: under the recipe's own ceiling, so the
     * vessel settles into the band rather than blowing through it on the way up (the same
     * {@link #FIRE_CONDUCTANCE}-dominates-leak reasoning as the Smoker's own doc). The numeric
     * coincidence with {@link #SMOKER_CEILING_TU} (380) is exactly that -- a coincidence, not a
     * shared constant -- because tempering's 400 and food's 400 are themselves unrelated figures
     * that happen to match.
     * <p>
     * This is also what makes the Annealing Furnace a genuinely different answer to controlled
     * cooling than a Crucible with a Damper (§5): it cannot be mismanaged into ruining the batch
     * by overheating, because it physically cannot reach hardening temperature at all -- the
     * trade is that safety for the Crucible route's actual control over the cooling rate.
     */
    public static final Tu ANNEALING_FURNACE_CEILING_TU = new Tu(380f);

    /**
     * Invented. How readily Work crosses a {@code HeatExchangerBlockEntity}, in the same {@code
     * Work/t/Tu} every other {@link Conductance} is in -- see {@code Heat#exchange}. Well under
     * {@link #FIRE_CONDUCTANCE} (0.6): a flame is in direct contact with a vessel, while two
     * vessels linked by an exchanger are trading heat through a wall built for the purpose, not
     * sitting in each other's fire.
     */
    public static final Conductance HEAT_EXCHANGER_CONDUCTANCE = new Conductance(0.1f);

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
     * Invented. Above this many {@link io.github.soundgoodizerfan.feedback.core.unit.Pu}, a
     * Pressure Vessel starts risking an explosion every tick, at a chance that climbs the further
     * past this line it sits -- read from PneumaticCraft: Repressurized's {@code
     * MachineAirHandler#addAir} (GPL-3.0, this project's own licence). Below it: perfectly safe,
     * however long it sits there. This is the hard gate that gives {@code
     * feedback_philosophy.md} §14's named GregTech-boiler-explosion precedent something to
     * actually happen to, closing the "exploding boiler" open item in {@code TODO.md} §5a for
     * Feedback's own equipment in general rather than for the Boiler specifically.
     */
    public static final Pu PRESSURE_VESSEL_DANGER_PU = new Pu(5f);

    /** Invented. At or above this many Pu, a Pressure Vessel explodes outright, every tick. */
    public static final Pu PRESSURE_VESSEL_CRITICAL_PU = new Pu(7f);

    /**
     * Invented. A Pressure Vessel is not perfectly sealed -- a small constant bleed, in the same
     * air units as {@link #PRESSURE_VESSEL_VOLUME}, so a vessel nobody is actively pumping settles
     * back to nothing rather than holding whatever it was last left at forever. Small enough that
     * a bellows worked at any real rate climbs faster than this drains.
     */
    public static final Mb PRESSURE_VESSEL_LEAK_PER_TICK = new Mb(1f);

    /** Invented. Jars a Pressure Vessel holds (food spec 8, Canning). */
    public static final int CANNING_SLOTS = 4;

    /**
     * Invented. Pressure raises the boiling point: the hottest the jars can get is {@link #POT_BOIL_TU}
     * plus this per Pu. At 6, {@link #CANNING_STERILISE_TU} is reached at under 2.7 Pu, well below
     * {@link #PRESSURE_VESSEL_DANGER_PU}, so the working window is about 2.7 to 5 Pu.
     */
    public static final float CANNING_BOIL_PER_PU = 6f;

    /** Invented. The jars' contents must be at or above this, in Tu, to sterilise: hotter than plain boiling, so only a pressure vessel gets there. */
    public static final Tu CANNING_STERILISE_TU = new Tu(116f);

    /** Invented. Ticks at or above {@link #CANNING_STERILISE_TU} to take a jar from raw to sealed (30 s). */
    public static final float CANNING_STERILE_TICKS = 600f;

    /** Invented. Ticks a sealed jar kept at sterilising heat takes to go from firm to full mush (2 min). */
    public static final float CANNING_MUSH_TICKS = 2400f;

    /** Invented. Mush at or above this shows as Mushy on the tooltip. */
    public static final float CANNING_MUSH_SHOWN_AT = 0.15f;

    /**
     * Invented. Ticks-equivalent of TPu lost per tick a hand-authored or vanilla-fallback
     * process spends outside its useful temperature range, per {@code docs/tpu_spec_doc.md}'s
     * "TPu May Decay" and "TPu Should Not Necessarily Be Monotonic" sections.
     *
     * <h3>Why one global constant rather than a per-process field</h3>
     * The doc calls the exact decay rule "a tuning decision", not a material property -- nothing
     * about how fast a batch forgets its progress is something a modpack author would credibly
     * want to retune per recipe. Set equal to the fastest possible accumulation rate (suitability
     * 1.0 = 1 TPu/t), so a process interrupted right at its optimum loses ground exactly as fast
     * as it could gain it -- no free ratchet in either direction.
     */
    public static final float TPU_DECAY_PER_TICK = 1f;

    /**
     * Invented. Where a {@code minecraft:smoking}-classified vanilla-fallback recipe accumulates
     * TPu fastest, in Tu -- {@code docs/tpu_spec_doc.md}'s "Smoking" thermal hint: food-like, prefers
     * lower and more stable heat. Sits well inside {@link #FOOD_MAX_TU}'s wall so a hold near
     * the optimum has real margin before the recipe burns instead of finishing.
     */
    public static final Tu SMOKING_OPTIMAL_TU = new Tu(250f);

    /**
     * Invented. Where a generic (non-blasting) metal-classified vanilla-fallback recipe -- the
     * {@code smelting} hint -- accumulates TPu fastest, in Tu. Set just above {@link
     * #METAL_MIN_TU} rather than deep into blasting territory: {@code docs/tpu_spec_doc.md}'s Furnace
     * section wants the generalist "capable of both bands, optimized for neither", and this is
     * the metal half of that mediocrity.
     */
    public static final Tu SMELTING_OPTIMAL_TU = new Tu(1000f);

    /**
     * Invented. Where a {@code minecraft:blasting}-classified vanilla-fallback recipe accumulates
     * TPu fastest, in Tu -- {@code docs/tpu_spec_doc.md}'s "Blasting" hint: metallurgical, rewards a
     * hotter environment. Above {@link #SMELTING_OPTIMAL_TU} on purpose: it is the whole reason a
     * hotter, better-insulated vessel (a blast furnace) ends up finishing ore faster than a plain
     * furnace without either block ever checking the other's identity -- see {@link
     * io.github.soundgoodizerfan.feedback.process.VanillaFallback} for where this is read.
     */
    public static final Tu BLASTING_OPTIMAL_TU = new Tu(1400f);

    /**
     * What a full draught of air multiplies a fuel's flame temperature by.
     *
     * <h3>Air multiplies, it does not replace</h3>
     * A cool fuel blown hard is still a cool fire, which is both true and necessary: a bellows
     * that set an absolute temperature would make every fuel identical and the fuel table would
     * stop meaning anything. What air sells is a <em>proportion</em> more out of whatever is
     * already burning.
     *
     * <h3>This factor is beat 2's difficulty</h3>
     * Charcoal burns at 1220 Tu (see the {@code fuel} table), which is glowing, obviously fierce,
     * and <b>below steel's 1420 Tu window</b>. So the fire alone cannot make steel and no amount
     * of patience changes that -- philosophy 7's hard gate, a genuine impossibility rather than a
     * slow version. Air is the only way across, and air is the only thing the bellows sells.
     * <p>
     * Fully blown, charcoal reaches about 1830 Tu, which is well past the 1540 Tu at which iron
     * burns. That asymmetry is deliberate (§6): the player's one actuator points at a temperature
     * that destroys the work, so holding the window means knowing when to <em>stop</em> asking,
     * which is the entire beat.
     */
    public static final float FULL_AIR_TEMPERATURE_FACTOR = 1.5f;

    /**
     * SLICE: lava is 1200 Tu and it does not cool. Not slowly. At all.
     *
     * <h3>No explanation is offered, deliberately</h3>
     * Philosophy 2 says the fantastical cannot be a footnote, and in this slice it lives in the
     * most mundane place available -- the fuel. A bucket of lava under a crucible is an infinite,
     * perfectly stable heat source that is <b>too cool for steel and impossible to turn off</b>,
     * and the player has walked past it since day one without asking. There is no lore entry and
     * no analysis machine. There is a player who now owns a thermometer and has started pointing
     * it at things, which is the correct first step of §2's observe-then-exploit and the whole of
     * somebody else's slice.
     * <p>
     * Sat deliberately between a bare charcoal fire and the steel window, so it is genuinely
     * useful and genuinely not enough.
     */
    public static final Tu FIRE_TU_LAVA = new Tu(1200f);

    /**
     * A lit campfire (any block in {@code minecraft:campfires} but the soul campfire): wood's
     * flame temperature, since it is a log fire. Melts tin, lead and zinc but sits under gold's and
     * copper's melt -- the safe fire, and it never runs out.
     */
    public static final Tu FIRE_TU_CAMPFIRE = new Tu(760f);

    /**
     * A lit soul campfire: the Nether's reward. Clears copper, bronze, brass, gold and silver with
     * margin (a vessel only approaches its fire), stays under cast iron, lava and steel.
     */
    public static final Tu FIRE_TU_SOUL_CAMPFIRE = new Tu(1050f);

    /**
     * Invented. How much air the firebox can hold, and how fast it consumes it.
     *
     * <h3>Why a buffer exists</h3>
     * Strokes arrive in lumps and a fire does not flare and die between them. A small buffer
     * smooths the gap; a large one would let the player bank air and blast on demand, which turns
     * a continuous actuator into a battery and removes the reason to keep the bellows running.
     * Ten ticks of full burn is enough for the first and not the second.
     */
    public static final Mb FIREBOX_MAX_AIR = new Mb(40f);
    public static final Mb FIREBOX_AIR_PER_TICK = new Mb(4f);

    // --- the bellows ------------------------------------------------------------------------

    /**
     * SLICE: 10 mB of air on a short throw, 30 on a long one.
     *
     * <h3>The same part, meaning the opposite thing</h3>
     * On the hammer the short throw is the strong option, because a lever trades distance for
     * force. A bellows wants displaced air rather than force, so the trade runs the other way and
     * the <em>long</em> throw is the useful one. The player has met this component already and
     * finds it inverted, which is the first time in the slice that one part composes two ways.
     * <p>
     * Sustaining a full fire takes {@link #FIREBOX_AIR_PER_TICK} air a tick, which is a long
     * throw at about 21 RPM -- or a short throw at 64 RPM, which beat 1 cannot reach. So the
     * wrong throw is not merely worse here, it cannot do the job at all.
     */
    public static final Mb BELLOWS_AIR_SHORT = new Mb(10f);
    public static final Mb BELLOWS_AIR_LONG = new Mb(30f);

    /**
     * SLICE: 40 Su, half a hammer.
     * <p>
     * Which is what makes the convergence tight. Beat 1's water wheel carries 256 Su; a hammer
     * fast enough to forge steel, a bellows keeping the fire in, and the shafting to reach both
     * is the first time the slice asks for most of that budget at once.
     */
    public static final Su BELLOWS_LOAD_SU = new Su(40f);

    /**
     * The most air one stroke can move, which is the long throw's figure and deliberately so.
     *
     * <h3>Gearing buys nothing here, and that is the physics</h3>
     * The bellows multiplies its throw's figure by the drive's gear advantage and then clamps to
     * this ceiling, the same arithmetic the hammer does in force. Setting the
     * ceiling <em>at</em> the long throw makes that clamp bite immediately, so no gear train ever
     * moves more air per stroke -- which is correct rather than a restriction. A bellows holds
     * what it holds; squeezing it harder does not find more air inside it.
     * <p>
     * So on the hammer gearing buys force, and on the bellows it buys only stroke rate. One
     * mechanism, two honest answers, because the ceiling is a property of the machine.
     */
    public static final Mb BELLOWS_MAX_AIR = BELLOWS_AIR_LONG;

    /**
     * Invented. The temperature at which the slice's bimetallic strip snaps over, in Tu.
     *
     * <h3>One figure, on the part, not on a dial</h3>
     * Philosophy 3 and 13: a strip is a piece of bent metal that trips where its two metals say
     * it trips. It has no setting, and wanting a different trip point means crafting a different
     * strip -- which is how this family grows and why the controller in slice 2 is a genuine
     * upgrade rather than a convenience.
     * <p>
     * Sat a third of the way up steel's 1420-1480 window rather than at either end, and the
     * figure was arrived at by simulating the loop rather than by taste. A large crucible under
     * full air climbs at 0.79 Tu/t and the strip reads every 20 ticks, so the loop oscillates
     * about 16 Tu either side of its trip point: 1440 gives 1424 to 1456, which fits. At 1425 the
     * bottom of the swing falls out of the window and the hold stalls for half of every cycle; at
     * 1470 the top of it does, and on a small crucible the top of the swing is 1599 and burns the
     * batch outright.
     */
    public static final Tu BIMETALLIC_TRIP_TU = new Tu(1440f);

    /**
     * SLICE: the crude thermometer resolves to 25 Tu.
     *
     * <h3>The number is chosen to be not quite enough</h3>
     * Steel's window is 60 Tu wide, so this splits it into two and a bit -- the player can tell
     * they are roughly in it and never exactly where. That is the point. An instrument that made
     * the window trivial would end the beat on purchase, and philosophy 8 is explicit that better
     * equipment narrows the distribution and never collapses it. A later thermometer buys
     * significant figures here and nothing else; it is one number.
     */
    public static final Tu THERMOMETER_RESOLUTION_TU = new Tu(25f);

    /**
     * Invented: the carried pressure gauge reads in half-Pu steps. The danger line is 5 Pu,
     * critical 7 and a boiler starts to seep at 3.5, so a half step lets the player see the
     * danger line coming without ever reading the exact figure.
     */
    public static final Pu PRESSURE_GAUGE_RESOLUTION_PU = new Pu(0.5f);

    /**
     * SLICE: a fitted temperature sensor resolves finer than the carried thermometer.
     * <p>
     * It is bolted to one vessel rather than carried everywhere, so the trade for that loss of
     * flexibility is a number a purpose-built fixture can actually hold. Still not zero -- §8's
     * noise floor applies here exactly as it does to every other instrument.
     */
    public static final Tu TEMPERATURE_SENSOR_RESOLUTION_TU = new Tu(10f);

    /**
     * SLICE: the crude calipers resolve a machine's condition to five per cent.
     *
     * <h3>Coarse on purpose, and coarser than the adjective in one place</h3>
     * The free bands are 2, 13, 20 and 35 points wide, so five per cent is finer than the eye
     * everywhere except the top -- a hammer reading 100% might be anywhere in the sound band. That
     * is the right failure: the figure is bought to tell a marked head from a battered one and to
     * watch the number move, not to detect the very first wasted blow. A later pair buys
     * significant figures here and nothing else.
     */
    public static final float CALIPERS_RESOLUTION_CONDITION = 0.05f;
    // Left a bare float on purpose. Instrument#resolution answers for every Quantity -- Fu as
    // well as Tu -- so its return type cannot be a temperature. The unit lives in the name here,
    // which is the honest place for it when the seam it crosses is quantity-agnostic.

    // --- readouts, thermal ------------------------------------------------------------------

    /**
     * Above this, the eye stops distinguishing anything, in Tu.
     *
     * <h3>A free sense has a range, like any other instrument</h3>
     * Philosophy 8 gives instruments six properties and range is the first of them, so it would be
     * strange for the one apparatus every player owns to have none. Past brilliant white, hotter
     * simply looks the same -- which matters in beat 2, because iron's spoil point at 1540 Tu is
     * <em>above</em> this. The player cannot see themselves crossing the line that ruins the
     * batch. That is not a trick; it is why the thermometer exists, and it is the difference
     * between a mod that hides a number and one that models why you cannot have it.
     */
    public static final Tu MAX_VISIBLE_TU = new Tu(1600f);

    /**
     * Where the free, qualitative temperature bands fall, in Tu -- each entry is the <em>top</em>
     * of a band, and {@link io.github.soundgoodizerfan.feedback.client.Readout} names them in order.
     *
     * <h3>Why they read as colours, and why there are this many</h3>
     * These are the bands a blacksmith actually works to, and that is not decoration -- it is the
     * honest answer to what a player's own senses give them. You cannot feel 1450 Tu; you can see
     * that iron has gone from bright red to orange, and you can be badly wrong about which.
     * <p>
     * The first version had six wide bands and steel's entire 60 Tu window sat inside one of them,
     * which made the free sense useless rather than coarse. TerraFirmaCraft uses eleven, and the
     * reason is worth stating: the bands should be fine enough that a player can <em>get close</em>
     * by eye and never fine enough to land a 60 Tu window reliably. That is the gap the
     * thermometer is sold into, and a gap that is merely "hot or not" is not a gap, it is a wall.
     * So the bands crowd where the work happens and are wide where nothing does.
     */
    public static final List<Tu> HEAT_BAND_TOPS = List.of(
            new Tu(200f),    // warm
            new Tu(480f),    // hot
            new Tu(580f),    // faint red
            new Tu(730f),    // dark red
            new Tu(930f),    // bright red
            new Tu(1100f),   // orange
            new Tu(1300f),   // yellow
            new Tu(1400f),   // pale yellow
            new Tu(1500f),   // white
            MAX_VISIBLE_TU);


    // --- control --------------------------------------------------------------------------

    /**
     * How long one wind of the Timer lasts, in ticks.
     * <p>
     * A short list of positions rather than a free number (§3). There is no correct value hiding
     * in here to be ground out -- the run being timed is not repeatable enough for one to exist
     * -- but a device with a scalar on it teaches players to hunt for one anyway, and that is a
     * habit worth not teaching.
     * <p>
     * The range is set against beat 1: a water wheel lands roughly a blow a second, and a plate
     * is five blows on the gentle crank. So the useful settings sit either side of that, and the
     * long ones exist mostly to demonstrate what overrun looks like.
     */
    public static final List<Integer> TIMER_SETTINGS = List.of(40, 100, 200, 400);

    // --- readouts -------------------------------------------------------------------------

    // Where the free, qualitative speed bands fall. Philosophy 8 says a player standing next to
    // a shaft can see roughly how fast it is going and nothing more precise than that, so these
    // are the only speed figures the game exposes without an instrument -- as words.
    //
    // The boundaries are set against what beat 1 can actually produce: a water wheel makes 4 RPM
    // per flowing side (4 to 16), a hand crank 32. So a one-sided wheel reads slow, a well sited
    // one reads as turning, and only the crank spins fast. Bands that all collapsed onto one
    // adjective would tell the player nothing.

    /** Below this a turning run reads as "turning slowly". */
    public static final Rpm TURNING_RPM = new Rpm(8f);
    /** At or above this a run reads as "spinning fast". */
    public static final Rpm SPINNING_FAST_RPM = new Rpm(24f);

    /**
     * Fraction of capacity above which a network reads as "straining".
     *
     * <h3>Why a warning band exists at all</h3>
     * Acceleration is surplus torque over inertia, so a network at 98% of capacity is not
     * slightly worse than one at 80% -- it takes eighty seconds to reach speed instead of two.
     * That is a real and useful behaviour, and it was completely invisible: the factory simply
     * felt broken. An adjective is the right granularity to fix that with, because it says
     * <em>where to look</em> without handing over the Su figures that calipers are for.
     */
    public static final float STRAINING_LOAD_FRACTION = 0.9f;

    // --- controller -------------------------------------------------------------------------

    /** A Controller's flat draw while turning -- reading and switching costs a little Su. */
    public static final Su CONTROLLER_LOAD_SU = new Su(2f);

    /**
     * A printed card's node cap. Not tuned to anything yet -- same honesty as
     * {@code DataNode.getMaxNodeLinks()}'s own comment.
     */
    public static final int CONTROLLER_MAX_NODES = 16;

    // --- Engine design (docs/feedback_vehicles_spec.md section 6, Engine Stand v1) --------------------
    // Every figure here is a placeholder (philosophy 19). Material figures are not here: they are
    // the material's own (material/*.json), per the spec's standing rule.

    /** Peak torque per litre of displacement. A 1.6 L engine makes about 160 Su. */
    public static final float ENGINE_SU_PER_MB = 100f;
    /**
     * Mean piston speed a material survives, in m/s, per sqrt(yield strength St / density Mu/mB):
     * steel ~20 m/s (a 4 x 80 x 80 redlines ~7500 RPM), copper ~9.7 (~3600). Placeholder.
     */
    public static final float ENGINE_PISTON_SPEED_PER_SQRT_SPECIFIC_YIELD = 14.5f;
    /** Where peak torque sits, as a fraction of redline. */
    public static final float ENGINE_PEAK_FRACTION = 0.6f;
    /** Metal in an engine, as a multiple of its displacement: a 2 L engine is 40 L of metal. */
    public static final float ENGINE_METAL_PER_MB = 20f;
    /** Ingots of the engine's material to build it from a blueprint. Cost is [OPEN] (spec section 6). */
    public static final int ENGINE_INGOTS_PER_CYLINDER = 1;

    // --- Gun design (docs/feedback_guns_spec.md sections 2 and 3, step 1) ----------------------------
    // Every figure here is a placeholder (philosophy 19). Material figures are not here: they are
    // the material's own (material/*.json). Pressure is Pu and shares a scale with a material's
    // yield strength, which is what lets a chamber's rating be read straight off the metal.

    /** Peak chamber pressure of the propellant in a full case, in Pu. Black powder; the figure a propellant material takes over (spec 3.6). */
    public static final float GUN_PROPELLANT_PEAK_PU = 3f;
    /** Work done on a projectile per Pu over one cubic metre. Anchors Pu to joules: 3 Pu is about 100 MPa. */
    public static final float GUN_WORK_PER_PU_M3 = 3.3e7f;
    /** Mean pressure behind the projectile over the barrel, as a fraction of the peak. */
    public static final float GUN_PRESSURE_FILL = 0.35f;
    /** Projectile length as a multiple of bore. */
    public static final float GUN_PROJECTILE_LENGTH_PER_BORE = 1.5f;
    /**
     * Shot (spec 12): a shell's projectile cut into pellets this wide (buckshot), at most this
     * many, thrown in a cone at least this wide whatever the barrel. Placeholder.
     */
    public static final float GUN_SHOT_PELLET_MM = 8f, GUN_SHOT_SPREAD_DEG = 4f;
    public static final int GUN_SHOT_MAX_PELLETS = 12;
    /**
     * A barrel as the drill first pierces a turned rod (spec 12): a pilot bore, the rod's outside,
     * its full length. Cut until the wall or the length is under these minimums and it is swarf.
     */
    public static final float GUN_BARREL_PILOT_MM = 3f, GUN_BARREL_STOCK_MM = 30f, GUN_BARREL_STOCK_M = 0.6f;
    public static final float GUN_BARREL_MIN_WALL_MM = 0.5f, GUN_BARREL_MIN_M = 0.05f;
    /**
     * A round in a bore it does not exactly fit (spec 12): it will not chamber more than
     * {@link #GUN_BORE_FIT_MM} off. Looser leaks gas -- work falls per mm, to a floor, and spread
     * grows; tighter squeezes it, and the peak pressure rises per mm. Placeholder.
     */
    public static final float GUN_BORE_FIT_MM = 1f, GUN_BORE_LEAK_PER_MM = 0.6f, GUN_BORE_LEAK_FLOOR = 0.3f,
            GUN_BORE_SPREAD_DEG_PER_MM = 3f, GUN_BORE_SQUEEZE_PER_MM = 1.5f;
    /** Calipers read a part's dimensions to this, in mm. */
    public static final float CALIPERS_RESOLUTION_MM = 0.1f;
    /** Propellant gas leaves with the projectile; this much extra momentum, as a fraction of the projectile's. */
    public static final float GUN_GAS_IMPULSE_FRACTION = 0.25f;
    /** Barrel bands, in metres (spec 3.3): short, medium, long. */
    public static final float GUN_BARREL_SHORT_M = 0.10f, GUN_BARREL_MEDIUM_M = 0.20f, GUN_BARREL_LONG_M = 0.40f;
    /** Chamber wall bands, in mm: thin, standard, thick. */
    public static final float GUN_WALL_THIN_MM = 1.5f, GUN_WALL_STANDARD_MM = 3f, GUN_WALL_THICK_MM = 5f;
    /**
     * The reference load's burn rate per metre of barrel (spec section 3.6): the fraction burned
     * by the muzzle is this times the barrel's length, capped at 1. The same figure as the
     * gunpowder material file, so the reference load is gunpowder's.
     */
    public static final float GUN_PROPELLANT_BURN_RATE_PER_M = 4.25f;
    /**
     * A case is drawn to scale with its bore, so its wall is this fraction of the bore's radius
     * and its rating is the metal's yield times it -- the same hoop-stress rule as a chamber, with
     * the bore cancelled. Brass's 5 then holds 3.75 Pu against gunpowder's nominal 3.
     */
    public static final float GUN_CASE_WALL_PER_RADIUS = 0.75f;
    /**
     * Overpressure ({@code docs/feedback_guns_spec.md} section 3.2). Gun damage is the ratio over the
     * rating times this, summed, and the gun is spent at 1: copper's 1.1 is about fifty shots,
     * lead's 9 is the first. The shooter is hurt only past the onset ratio, a band read off the
     * spec sheet, by this many hp per unit over it, never more than the cap. Each event is
     * multiplied by 1 plus or minus the noise (the floor philosophy 8 keeps inside any band).
     */
    public static final float GUN_OVERPRESSURE_DAMAGE_PER_RATIO = 0.15f;
    public static final float GUN_HAZARD_SHOOTER_ONSET = 1.5f, GUN_HAZARD_SHOOTER_HP_PER_RATIO = 6f,
            GUN_HAZARD_SHOOTER_MAX_HP = 16f, GUN_HAZARD_NOISE = 0.25f;
    /** Charge items that make the propellant's nominal peak pressure, and the most a case will take. */
    public static final int GUN_CHARGE_NOMINAL = 4, GUN_CHARGE_MAX = 8;
    /**
     * The Mixer's drum (guns spec 13.4b). Placeholders. Turns to blend a load to uniform; past that
     * the drum grinds the finished blend, so the grains break down (uniformity falls toward its
     * floor over {@code MIXER_SEGREGATE_TURNS}) and each turn adds {@code MIXER_FRICTION_TU} of heat.
     * Heat above ambient reaching the blend's lowest ignition temperature sets it off.
     */
    public static final float MIXER_BLEND_TURNS = 40f, MIXER_SEGREGATE_TURNS = 60f, MIXER_UNIFORMITY_FLOOR = 0.5f,
            MIXER_FRICTION_TU = 0.5f;
    public static final Su MIXER_LOAD_SU = new Su(20f);
    /** The survey meter (mechanics 9.5): reach in blocks, the step its ore count reads to, and the wait between uses. Placeholders. */
    public static final int SURVEY_RADIUS = 16, SURVEY_COOLDOWN_TICKS = 40;
    public static final float SURVEY_RESOLUTION = 8f;
    /** Invented. Oil reads as a block count would: this many per block that the nearest basin's circle reaches into the survey radius. */
    public static final float SURVEY_OIL_PER_BLOCK = 4f;
    /** Rounds each magazine band holds (spec 12); standard is what every magazine held before bands. Placeholder. */
    public static final int GUN_MAGAZINE_SMALL = 6, GUN_MAGAZINE_STANDARD = 10, GUN_MAGAZINE_EXTENDED = 20,
            GUN_MAGAZINE_DRUM = 50, GUN_MAGAZINE_BELT = 100;
    /** Rounds a gun's own feed holds: a revolver's cylinder, and a tube by its barrel's length. */
    public static final int GUN_CYLINDER_ROUNDS = 6, GUN_TUBE_ROUNDS_SHORT = 4, GUN_TUBE_ROUNDS_MEDIUM = 6,
            GUN_TUBE_ROUNDS_LONG = 8;
    /** Wall-clock ms a magazine swap takes, during which the gun does not fire. */
    public static final int GUN_RELOAD_MS = 1400;
    /** Wall-clock ms to put one round into a gun's own feed, and a reload is that per round. */
    public static final int GUN_RELOAD_ROUND_MS = 450;
    /** Work rate per level of Haste (vanilla's mining-speed step), and the same less per level of Mining Fatigue; the rate never falls below the floor. */
    public static final float HASTE_RATE_PER_LEVEL = 0.2f, HASTE_RATE_FLOOR = 0.2f;
    /**
     * A gun can be held in one hand if its own metal weighs no more than this (a straight arm carries it)
     * and its recoil speed (impulse over mass, m/s) is no more than this (the wrist holds it). Placeholders.
     */
    public static final float GUN_ONE_HAND_MAX_MU = 4f, GUN_ONE_HAND_MAX_RECOIL_MS = 6f;
    /** Fired one-handed, spread and kick grow by up to this fraction of themselves, in proportion to how near the gun is to the recoil limit. */
    public static final float GUN_ONE_HAND_SPREAD = 0.6f, GUN_ONE_HAND_KICK = 0.6f;
    /** A reload takes this many times as long with a gun in each hand: the other one has to be held out of the way. */
    public static final float GUN_DUAL_RELOAD_MULT = 2.5f;
    // --- Heat, bloom, cook-off, wear (spec steps 4; sections 3.4 and 6). Placeholders. ---------
    /** Work to warm a gun by one Tu, per Mu of its own metal: a 3 Mu pistol is 36 Work/Tu. */
    public static final float GUN_THERMAL_MASS_PER_MU = 12f;
    /** Work a barrel sheds per tick per Tu above ambient, over its thermal mass: heavy barrels cool slowly. */
    public static final float GUN_COOLING_WORK_PER_TICK = 9f;
    /**
     * Heat dumped in the barrel per Work delivered to the bullet. Most of a charge's energy is not
     * the bullet's, but most of what is not leaves with the gas: was 2, which put about 28 Tu into
     * a pistol per shot and smoked it in nine. A real steel barrel warms a few degrees a shot.
     */
    public static final float GUN_HEAT_PER_WORK = 0.3f;
    /** Propellant ignition temperature when a round names none (the reference load's). */
    public static final float GUN_PROPELLANT_IGNITION_TU = 300f;
    /** Spread is this many times wider again at the ignition temperature than cold (a soft consequence, never a lockout). */
    public static final float GUN_BLOOM_AT_IGNITION = 1.5f;
    /** Bore erosion per Work delivered, times (1 + how hot the barrel is as a fraction of ignition). */
    public static final float GUN_WEAR_PER_WORK = 4.5e-7f;
    /** A worn gun bottoms out here (philosophy 8: a worse gun, never an empty one). */
    public static final float GUN_CONDITION_FLOOR = 0.4f;
    /** At the floor a gun delivers this much less energy; spread opens by 1 / condition. */
    public static final float GUN_WEAR_ENERGY_LOSS = 0.3f;
    /** A cooked-off round hurts the shooter this much per nominal charge, capped. */
    public static final float GUN_COOKOFF_HP = 2f, GUN_COOKOFF_HP_MAX = 8f;
    // --- What a hit does (spec 3.5). Placeholders. ---------------------------------------------
    /**
     * A block whose material has a toughness at or under this is brittle enough for a bullet to
     * break (glass 1, ice 1; stone 6, obsidian 2 are not). It is the marker figure the author
     * ruled on, not a list: a pack's pane qualifies because of what it is.
     */
    public static final float GUN_BREAK_TOUGHNESS_MAX = 1.5f;
    /** Work a bullet gives up to break a block, per unit of the block's toughness. */
    public static final float GUN_BREAK_WORK_PER_TOUGHNESS = 40f;
    /**
     * Momentum a bullet needs to break a brittle block, per unit of its toughness, in kg m/s
     * (spec 3.5, after Create Big Cannons: what breaks a block is a blow, so momentum, and what it
     * costs is work). A 9 mm lead pistol round carries about 2; glass is toughness 1.
     */
    public static final float GUN_BREAK_MOMENTUM_PER_TOUGHNESS = 0.5f;
    /**
     * A bullet softer than the block it meets glances off when it comes in shallower than this:
     * the cosine of the angle to the surface normal (about 70 degrees off it). It keeps
     * {@code GUN_RICOCHET_KEEP} of its energy, at most {@code GUN_RICOCHET_MAX} times.
     */
    public static final float GUN_RICOCHET_MAX_COS = 0.34f, GUN_RICOCHET_KEEP = 0.5f;
    public static final int GUN_RICOCHET_MAX = 2;
    /** The projectile's hardness when a shot names no material: lead's. */
    public static final float GUN_DEFAULT_PROJECTILE_HARDNESS = 0.3f;
    /** A bullet that has less than this share of its muzzle energy left drops where it is. */
    public static final float GUN_MIN_ENERGY_FRACTION = 0.03f;
    // --- Handling: aiming, stance, fire modes, recoil climb (spec 7). Placeholders. -------------
    /** Spread multiplier when aimed down the sights, and the kick that is left of it. */
    public static final float GUN_ADS_SPREAD = 0.4f, GUN_ADS_KICK = 0.65f;
    /** Spread multipliers by what the shooter is doing (spec 7.1: TacZ's five stances, as multipliers). */
    public static final float GUN_STANCE_CROUCH = 0.8f, GUN_STANCE_MOVING = 1.6f, GUN_STANCE_SPRINT = 2f,
            GUN_STANCE_AIRBORNE = 3f;
    /** Blocks per tick of horizontal travel past which a shooter is moving. */
    public static final double GUN_MOVING_SPEED = 0.05;
    /** Rounds in a burst. */
    public static final int GUN_BURST_ROUNDS = 3;
    /** Kick grows this much per shot of a streak, up to {@link #GUN_KICK_CLIMB_MAX} shots. */
    public static final float GUN_KICK_CLIMB_PER_SHOT = 0.12f;
    public static final int GUN_KICK_CLIMB_MAX = 10;
    /** Plates a sight is built from at assembly: iron sights are cut in the slide, a reflex one, a scope two. */
    public static final int GUN_SIGHT_PLATES_IRON = 0, GUN_SIGHT_PLATES_REFLEX = 1, GUN_SIGHT_PLATES_SCOPE = 2;
    /** FOV multiplier aimed through each sight, and ms to come up to it. */
    public static final float GUN_SIGHT_FOV_IRON = 0.85f, GUN_SIGHT_FOV_REFLEX = 0.8f, GUN_SIGHT_FOV_SCOPE = 0.4f;
    public static final int GUN_SIGHT_MS_IRON = 120, GUN_SIGHT_MS_REFLEX = 150, GUN_SIGHT_MS_SCOPE = 300;
    // --- Blends, coil drive, payloads (spec 3.6, 3.7). Placeholders. ---------------------------
    /** A charge that is mostly fuel still gives this much of its pressure: a fizzle, not a nothing. */
    public static final float GUN_FIZZLE_FLOOR = 0.1f;
    /** Work per redstone dust at a medium coil with a magnetic slug: four dust is about a pistol's shot. */
    public static final float GUN_COIL_WORK_PER_DUST = 110f;
    /** A non-magnetic slug barely couples to the field (eddy currents, not pull). */
    public static final float GUN_COIL_NONMAGNETIC_COUPLING = 0.1f;
    /** Stages add up with coil length but saturate: the most a long coil gets over a medium one. */
    public static final float GUN_COIL_STAGE_MAX = 1.6f;
    /**
     * Spring launcher (spec 13.4). Work stored goes as stress squared over the stock wound, times the
     * stroke the barrel gives: tempered steel's single spring in a medium barrel is about a third of a
     * pistol's shot, iron's is next to nothing. Recocking costs the work back, by hand, per work.
     */
    public static final float GUN_SPRING_WORK_PER_ST2 = 0.19f, GUN_SPRING_STROKE_MAX = 1.6f, GUN_SPRING_COCK_MS_PER_WORK = 3f;
    /** Metal in one coil's worth of spring stock, mB. */
    public static final float GUN_SPRING_METAL_MB = 30f;
    /**
     * Steam launcher (spec 13.4). A full chest holds the boiler's relief line; the chest's rating is its metal's
     * yield strength times this; steam gives a fraction of what powder at the same pressure does, because there is
     * no combustion in it; a shot lets out a fixed volume, so a bigger chest is more shots and not a stronger one;
     * and the steam in a carried gun condenses away.
     */
    public static final float GUN_STEAM_FULL_PU = 3f, GUN_STEAM_RATING_PER_ST = 0.2f, GUN_STEAM_WORK_FRACTION = 0.4f,
            GUN_STEAM_BLAST = 0.3f;
    public static final int GUN_STEAM_MB_PER_SHOT = 100;
    public static final float GUN_STEAM_LEAK_MB_PER_TICK = 0.25f;
    /** Metal in a chest of the reference size, mB, and that size. */
    public static final float GUN_STEAM_METAL_MB = 40f, GUN_STEAM_CHEST_REF_MB = 400f;
    /**
     * Demolition charge (guns spec 13.4a). Power goes as the square root of the blend's burned
     * potential: sixteen gunpowder bare is about a block of TNT, tempered steel round it nearly
     * twice that. Casing confinement is yield strength times this, to a cap; a charge that burns
     * under this fraction throws fire; one under the minimum power only smokes.
     */
    public static final int DEMO_FILLER_MAX = 16, DEMO_FUSE_TICKS = 60;
    public static final float DEMO_POWER_PER_SQRT = 1.15f, DEMO_POWER_MAX = 8f, DEMO_POWER_MIN = 0.5f,
            DEMO_CONFINE_PER_ST = 0.03f, DEMO_CONFINE_MAX = 1f, DEMO_FIRE_BELOW_BURNED = 0.9f;
    /** An explosive round carries one item's worth of filler per 9 mm bore volume, scaled by this; it arms after this many ticks of flight (point blank is a dud). */
    public static final float DEMO_ROUND_SCALE = 0.6f;
    public static final int DEMO_ROUND_ARM_TICKS = 2;
    /** Redstone blocks the winding of each coil length takes. */
    public static final int GUN_COIL_BLOCKS_SHORT = 1, GUN_COIL_BLOCKS_MEDIUM = 2, GUN_COIL_BLOCKS_LONG = 3;
    /**
     * A charged coil round (spec 3.7, mechanics 6.6): the volts that drive a coil as hard as a
     * nominal redstone charge. MV, so a dynamo at a steam engine's speed on its fine winding fills
     * a round to nominal, a cell to a fifth of it, and a hand-cranked fine dynamo past it -- into
     * the winding's rating. Volts over this go as the square in field pressure, as dust does.
     */
    public static final float GUN_COIL_NOMINAL_VOLTS = 6f;
    /** Amp-ticks a round takes per volt: a round is a small jar. At one amp, nominal in 12 ticks. */
    public static final float GUN_ROUND_CAPACITANCE = 2f;
    /**
     * Volts a charged round loses a tick, wherever it is: linear, like a workpiece's heat, so
     * "this round is flat in five minutes" is literally true (nominal to nothing in 6000 t).
     */
    public static final float GUN_ROUND_LEAK_VOLTS_PER_TICK = 0.001f;
    /** Current the charger asks for while anything in it is below the line. */
    public static final float GUN_CHARGER_AMPS = 1f;
    /** Seconds a target burns from an incendiary hit. */
    public static final int GUN_INCENDIARY_SECONDS = 5;
    /**
     * Metal in each part off the barrel, in mB: receiver, slide (or bolt), trigger group and grip,
     * each in its own material (spec 2, "a gun is parts"). They were one 0.3 mB frame in the
     * barrel's metal, and still add up to it.
     */
    public static final float GUN_RECEIVER_METAL_MB = 0.08f, GUN_SLIDE_METAL_MB = 0.07f, GUN_TRIGGER_METAL_MB = 0.03f,
            GUN_GRIP_METAL_MB = 0.12f;
    // --- Fittings are data: data/feedback/gun_option/*.json (spec 2, 11 step 7). Placeholders. ---
    /**
     * The report is muzzle blast (gas: the gun's, a muzzle device acts on it) and the projectile's
     * crack (its speed against sound: the round's, nothing on the gun touches it). The crack is this
     * share of a supersonic shot's report and ramps in across a tenth either side of the speed of sound.
     */
    public static final float GUN_CRACK_SHARE = 0.4f, SOUND_MS = 343f, GUN_CRACK_RAMP = 0.1f;
    /**
     * Eddy sleeve: a bullet that is not magnetic couples to the sleeve's field this much; no sleeve
     * drains more than this share of the muzzle energy; the drained energy is this many times itself in extra heat.
     */
    public static final float GUN_EDDY_NONMAGNETIC_COUPLING = 0.1f, GUN_EDDY_MAX_DRAIN = 0.75f, GUN_EDDY_HEAT = 1.5f;
    /** A report below this is a quiet vibration, heard by sculk at a short range. */
    public static final float GUN_QUIET_REPORT = 0.5f;
    /** Placeholder. A report at or above this is thunderous: sculk hears it from twice as far as a loud one. */
    public static final float GUN_THUNDEROUS_REPORT = 0.95f;
    /** Metal in a revolver's cylinder and in a tube magazine, in mB: the feed's own part, when it has one. */
    public static final float GUN_CYLINDER_METAL_MB = 0.06f, GUN_TUBE_METAL_MB = 0.04f, GUN_BELT_METAL_MB = 0.05f, GUN_BOX_METAL_MB = 0.04f;
    /** Cycle time: a fixed action plus time per unit of impulse (kg m/s), in ms. */
    public static final int GUN_CYCLE_BASE_MS = 90;
    /**
     * Time a hand spends working a manual action between shots, in ms (spec 12): a break action
     * is the least (the next barrel is already loaded), a bolt the most. Placeholder.
     */
    public static final int GUN_CYCLE_MANUAL_MS_BREAK = 150, GUN_CYCLE_MANUAL_MS_LEVER = 450,
            GUN_CYCLE_MANUAL_MS_PUMP = 550, GUN_CYCLE_MANUAL_MS_BOLT = 800;
    /**
     * Was 30, which made a big-bore automatic cycle at about 450 ms (130 rounds a minute). An
     * action's rate is mostly its own mechanism; impulse only slows it a little.
     */
    public static final float GUN_CYCLE_MS_PER_IMPULSE = 6f;
    /** Spread at a 0.2 m barrel, in degrees, scaling inversely with length down to a floor that is never zero (philosophy 8). */
    public static final float GUN_SPREAD_AT_MEDIUM_DEG = 2f, GUN_SPREAD_FLOOR_DEG = 0.25f;
    /** Camera kick per unit of impulse, in degrees, and how long it takes to land. */
    public static final float GUN_KICK_DEG_PER_IMPULSE = 1.1f;
    /**
     * Kick is read against a reference gun: {@link #GUN_KICK_DEG_PER_IMPULSE} is what a gun this
     * heavy with a grip this grippy kicks per impulse (a steel 9 mm, medium barrel, standard wall
     * weighs about 2.6 Mu; steel's friction is 0.6), and kick goes as reference over actual for both.
     */
    public static final float GUN_KICK_REFERENCE_MU = 2.6f, GUN_KICK_REFERENCE_FRICTION = 0.6f;
    public static final int GUN_KICK_MS = 90;
    /** Damage per Work delivered. */
    public static final float GUN_DAMAGE_PER_WORK = 0.04f;
    /** Air drag fraction of speed lost per tick, and in a fluid; gravity in blocks per tick squared (9.8 m/s² over 400). */
    public static final float GUN_DRAG_PER_TICK = 0.005f, GUN_FLUID_DRAG_PER_TICK = 0.35f, GUN_GRAVITY_PER_TICK = 0.0245f;
    /** A shot that has not hit anything by now is gone. */
    public static final int GUN_MAX_LIFE_TICKS = 80;
    /** How early the server accepts a shot against the cycle, for network jitter. Timestamp sync replaces this (spec 7.1). */
    public static final int GUN_FIRE_TOLERANCE_MS = 100;
    /**
     * Armed skeletons ({@code docs/spec_armed_skeletons.md}). Chance a newly spawned skeleton holds a gun; rounds it
     * carries beyond what is loaded; the multiplier on a gun's spread for a mob's untrained hands (never a perfect
     * aim); the drop in blocks of a flat-fired round past which a skeleton will not take the shot (its effective
     * range is worked out from the gun's muzzle speed, not set). Placeholders.
     */
    public static final float SKELETON_GUN_CHANCE = 0.03f;
    /** Chance a newly spawned wither skeleton holds a gun (same early pool; the Nether's ambient heat is the difference). Placeholder. */
    public static final float WITHER_SKELETON_GUN_CHANCE = 0.06f;
    public static final int SKELETON_GUN_SPARE = 6;
    public static final float SKELETON_GUN_STANCE = 2.5f;
    public static final float SKELETON_GUN_DROP_BLOCKS = 3f;
    /**
     * Wind-up before each shot, announced by a sound and sparks at the muzzle so a player can parry (20 t, 1 s);
     * the least gap between one shot and the next wind-up, whatever the gun's own cycle (40 t, 2 s); how far off
     * the aim the head may point and still count as aiming. Placeholders.
     */
    public static final int SKELETON_GUN_WINDUP_TICKS = 20;
    public static final int SKELETON_GUN_MIN_INTERVAL_TICKS = 40;
    public static final float SKELETON_GUN_FACING_DEG = 10f;
    /**
     * Aim error of a skeleton's shot, in degrees: each shot is aimed off the target by up to this much in a
     * random direction, on top of the gun's spread, so a target standing still is missed often (about half
     * the time at 10 blocks) and more at range. Placeholder.
     */
    public static final float SKELETON_GUN_AIM_ERROR_DEG = 3f;
    /**
     * The gun-fu melee stance (combat plan section 0, "Gun-fu first move"): a gun swung as a bludgeon. Damage is
     * {@code BASE + PER_MU x gunMass} capped at MAX, so a heavy gun hits harder and nothing is keyed on a
     * gun's identity. The cooldown is {@code TICKS_PER_MU x gunMass} between MIN and MAX ticks: a heavy gun
     * is slow to bring round again. Knockback is {@code BASE + PER_MU x gunMass}. A gun at or over
     * HEAVY_MU lands with the strong attack sound, a lighter one with the weak. Placeholders, to be tuned in play.
     */
    public static final float GUN_BLUDGEON_DAMAGE_BASE = 1f, GUN_BLUDGEON_DAMAGE_PER_MU = 1f, GUN_BLUDGEON_DAMAGE_MAX = 10f;
    public static final float GUN_BLUDGEON_TICKS_PER_MU = 4f;
    public static final int GUN_BLUDGEON_TICKS_MIN = 8, GUN_BLUDGEON_TICKS_MAX = 30;
    public static final float GUN_BLUDGEON_KNOCKBACK_BASE = 0.3f, GUN_BLUDGEON_KNOCKBACK_PER_MU = 0.08f, GUN_BLUDGEON_HEAVY_MU = 4f;
    /**
     * A bludgeon hit staggers when its impact ({@code damage x ParryTuning.JOULES_PER_DAMAGE}) over the target's
     * stand-in mass gives at least this many raw stagger ticks (the parry's formula, unclamped); the target is
     * staggered for that many ticks, bounded as the parry's, and the gun then fires one free shot at it.
     * A 3 Mu gun (4 damage, 40 J) on a zombie (about 84 Mu) gives 8 ticks, so it staggers; a 1 Mu gun
     * (2 damage) gives 4 and does not. Placeholder.
     */
    public static final float GUN_BLUDGEON_STAGGER_MIN_TICKS = 6f;
    /** Half-width of the bludgeon's swing line, blocks: a gun is a narrow head. */
    public static final float GUN_BLUDGEON_HALF_WIDTH = 0.3f;
    /** Gun-fu spin (skill unlock): a sneaking swing hits every body within this many blocks, and the longer cooldown it costs (multiplier). Placeholder. */
    public static final double GUN_SPIN_RADIUS = 2.0;
    public static final float GUN_SPIN_COOLDOWN_MULT = 1.5f;
    /** Muzzle energy that reads as full power for sound pitch and volume. */
    public static final float GUN_SOUND_REFERENCE_WORK = 1000f;

    // --- electricity (feedback_mechanics.md section 6) ----------------------------------------
    //
    // Every figure here is a PLACEHOLDER (philosophy 19). The band anchors are [OPEN] (6.3): the
    // reference volts below only give the four named bands an order of magnitude to be compared
    // against, a factor of four apart, and carry no claim about what a band is physically.
    // Voltages are plain volts in floats: a band is a rating (VoltageBand) and what a sink sees
    // is a continuous figure after path drop, which no closed list can name.

    /** Placeholder. Reference volts of each {@link io.github.soundgoodizerfan.feedback.core.unit.VoltageBand}. */
    public static final float ELECTRIC_LV_VOLTS = 1.5f, ELECTRIC_MV_VOLTS = 6f,
            ELECTRIC_HV_VOLTS = 24f, ELECTRIC_UHV_VOLTS = 96f;

    /**
     * Placeholder. Amps a wire carries per unit of its material's {@code ampacity} (copper = 1),
     * so copper carries 4 A. Resistance is not modelled; ampacity is the whole of "how much".
     */
    public static final float WIRE_AMPS_PER_AMPACITY = 4f;

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

    /**
     * Placeholder. Air gap, in blocks, a bare conductor arcs across to another bare
     * conductor in a straight line (6.4). Structural, not a dial: the neighbour walk asks about
     * exactly one air block, so this stays 1 unless that walk is widened with it.
     */
    public static final int ARC_GAP_BLOCKS = 1;

    /** Placeholder. Ticks between sparks (and their crackle) on an arc that is carrying. */
    public static final int ARC_SPARK_INTERVAL_TICKS = 20;

    /** Placeholder. Damage a bare, loaded conductor does to what touches it, twice a second. */
    public static final float BARE_WIRE_SHOCK_DAMAGE = 1f;

    /**
     * Placeholder. Conductor condition lost per tick at a temperature twice its material's
     * service temperature, scaled linearly by how far past service it is (6.4 stage 2): zero at
     * service, this much at 2x. 0.002 means a conductor held at 2x service is gone in 500 ticks.
     */
    public static final float WIRE_DEGRADE_PER_TICK = 0.002f;

    /**
     * Placeholder, and [OPEN] (6.2): how far past its rated current a source can be drawn, as a
     * multiple of that rating, before it supplies no more. Below the rating the source holds its
     * voltage; between the rating and this it sags (see {@link #SOURCE_SAG_FLOOR}). Never destroyed.
     */
    public static final float SOURCE_OVERDRAW_LIMIT = 2f;

    /** Placeholder. Amps a galvanic cell holds its voltage up to: a curiosity, not a supply. */
    public static final float GALVANIC_CELL_AMPS = 1f;
    /** Cells in a galvanic pack (zinc against copper, 1.1 V each): 5.5 V, under the MV reference. */
    public static final int GALVANIC_PACK_CELLS = 5;
    /** Placeholder. Tu past a part's service temperature that costs one flaw when it is dipped (up to the flaw cap). */
    public static final float DIP_FLAW_TU = 300f;
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
    public static final float ELECTROSTATIC_VOLTS = ELECTRIC_HV_VOLTS;
    public static final float ELECTROSTATIC_AMPS = 0.5f;
    public static final int ELECTROSTATIC_PULSE_TICKS = 20;

    /**
     * Placeholder. What each electrical instrument can resolve (philosophy 8: tiers buy
     * significant figures, never access). The electroscope is coarse on purpose -- it says a wire
     * is live and roughly how hard, not how much is flowing (mechanics 6.7).
     */
    public static final float GALVANOMETER_RESOLUTION_AMPS = 0.25f;
    public static final float CURRENT_SENSOR_RESOLUTION_AMPS = 0.5f;
    public static final float ELECTROSCOPE_RESOLUTION_VOLTS = 0.5f;

    /**
     * Placeholder. The electromagnet (mechanics 6.5). It wants one cell's worth of current, rated
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
    public static final float ELECTROMAGNET_PULL = 0.08f;
    public static final int ELECTROMAGNET_PULL_INTERVAL_TICKS = 2;

    /**
     * Placeholder. One ingot of any metal, in {@link Qu} (philosophy 17: a mole stand-in, so every
     * metal's ingot is the same amount). A nugget is a ninth of it, a block nine times it.
     */
    public static final Qu QU_PER_INGOT = new Qu(10f);

    /**
     * Placeholder. The electrolytic cell (mechanics 6.14). Current is the rate, so this is the one
     * figure that turns amps into metal: Qu moved per amp per tick (Faraday's constant, in this
     * mod's units; no type, as {@code Work} has none). At {@link #ELECTROLYTIC_CELL_AMPS} an ingot
     * takes 600 t (30 s); a galvanic cell's single amp takes 4800 t (4 min).
     */
    public static final float QU_PER_AMP_TICK = 1f / 480f;
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
     * it the whole deposit is that metal's scrap. Spoil only for now (mechanics 6.14).
     */
    public static final float ELECTROLYTIC_PURE_FRACTION = 0.95f;

    /**
     * Placeholder. Electroplating (mechanics 6.15): the window for a coat, in Qu on one piece. Under
     * the first it has not closed (porous); over the second it is too thick to hold and flakes off
     * as it comes out of the water. At a cell's single amp the window is about 12 s to 48 s; at a
     * fine dynamo's two, a quarter of that sooner and narrower.
     */
    public static final float PLATING_POROUS_BELOW_QU = 0.5f;
    public static final float PLATING_SPALLS_ABOVE_QU = 2f;

    /**
     * Placeholder. The mass probe (mechanics 6.17): per cut of crystal plate, the most coat it reads
     * (the range, in Qu) and the smallest step it tells apart. A thin cut is fine and short, a thick
     * one reaches far and reads coarse. Every range sits under {@link #PLATING_SPALLS_ABOVE_QU}, so
     * a probe is pegged before the coat on it spalls, which is the trap.
     */
    public static final float MASS_PROBE_RANGE_COARSE_QU = 1.9f;
    public static final float MASS_PROBE_RANGE_STANDARD_QU = 1.2f;
    public static final float MASS_PROBE_RANGE_FINE_QU = 0.6f;
    public static final float MASS_PROBE_STEP_COARSE_QU = 0.1f;
    public static final float MASS_PROBE_STEP_STANDARD_QU = 0.05f;
    public static final float MASS_PROBE_STEP_FINE_QU = 0.01f;
    /** Placeholder. Noise floor, in steps: even a sound probe reads a little off (philosophy 8). */
    public static final float MASS_PROBE_NOISE_STEPS = 0.4f;
    /** Placeholder. Extra noise, in steps, for a probe cracked to its floor. */
    public static final float MASS_PROBE_CRACKED_NOISE_STEPS = 6f;
    /** Placeholder. Over-voltage cracks the crystal: condition lost per volt over the cell's rating, per tick. */
    public static final float MASS_PROBE_WEAR_PER_EXCESS = 0.002f;
    public static final float MASS_PROBE_CONDITION_FLOOR = 0.5f;

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
     * Placeholder. The heating coil (mechanics 6.16). The current it asks for, and Joule's law in
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
     * Placeholder. The breaker (mechanics 6.4): it trips above a copper cable's rating, the thing
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

    /** Placeholder, and [OPEN] (6.2): the fraction of its voltage a source holds at {@link #SOURCE_OVERDRAW_LIMIT}. */
    public static final float SOURCE_SAG_FLOOR = 0.25f;

    /**
     * Placeholder. The dynamo's two windings (mechanics 6.8): volts per RPM of the input shaft,
     * which is also the Su each amp drawn costs that shaft, and the amps it holds its voltage up
     * to. Chosen so the two carry the same power at any speed (0.75 x RPM either way) and neither
     * dominates, and so the band anchors fall out of the machine rather than being set: a steam
     * engine's {@link #STEAM_ENGINE_RPM} makes {@link #ELECTRIC_LV_VOLTS} on the coarse winding and
     * {@link #ELECTRIC_MV_VOLTS} on the fine one. A faster shaft makes more -- gear it up.
     */
    public static final float DYNAMO_COARSE_VOLTS_PER_RPM = ELECTRIC_LV_VOLTS / 16f;
    public static final float DYNAMO_COARSE_AMPS = 8f;
    public static final float DYNAMO_FINE_VOLTS_PER_RPM = ELECTRIC_MV_VOLTS / 16f;
    public static final float DYNAMO_FINE_AMPS = 2f;

    /**
     * Placeholder. The Leyden jar (mechanics 6.6). Capacitance in amp-ticks per volt: one
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

    // --- the steam loop ---------------------------------------------------------------------

    /**
     * Invented. The condenser: tank sizes, the most it condenses a tick (matched to an engine's
     * {@link #STEAM_ENGINE_MB_PER_TICK}), and its body. Every mB condensed puts
     * {@link #BOILER_WORK_PER_MB_STEAM} back into that body and it stops at
     * {@link #BOILER_WORKING_TU}, so on its own leak it settles at a trickle: at ambient 20 it sheds
     * 0.05 x 100 = 5 Work a tick, a quarter of an mB. Taking its heat away is the player's problem.
     */
    public static final int CONDENSER_TANK_MB = 1000;
    public static final int CONDENSER_MB_PER_TICK = STEAM_ENGINE_MB_PER_TICK;
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

    // --- driving ----------------------------------------------------------------------------

    /**
     * Invented. A tire is its own spring until a built spring exists to hang it from
     * ({@code docs/feedback_vehicles_spec.md} section 5): it rings at {@code TIRE_SPRING_HZ} under its
     * share of the car, at {@code TIRE_DAMPING_RATIO} of critical. A frequency, not a stiffness, so a
     * heavy car squats exactly as far as a light one -- {@code g / (2 pi f)^2}, about 3 cm.
     */
    public static final float TIRE_SPRING_HZ = 3f;
    /** Invented. A hub's suspension band (soft, stock, stiff): the tire's ring frequency, so a banded choice, not a scroll. Stock is {@link #TIRE_SPRING_HZ}. */
    public static final float[] SUSPENSION_HZ = {2f, TIRE_SPRING_HZ, 4.5f};
    public static final float TIRE_DAMPING_RATIO = 0.7f;
    /**
     * Invented. The share of a tire's slip it cancels in one physics step, before grip caps it. Below
     * one so a stiff coupling stepped at a fixed rate cannot overshoot and ring (spec section 5,
     * <em>Stability</em>).
     */
    public static final float TIRE_SLIP_RESPONSE = 0.5f;
    /**
     * Rolling resistance, a fraction of a tire's load, is deformation: the tire flexing as it rolls
     * and the ground giving under it. Each side is {@code ROLLING_SOFT / (1 + hardness /
     * ROLLING_HARDNESS)} of its own material -- placeholder curve through real points: steel on
     * rail about 0.002, a soft tire about 0.1, dirt as ground about 0.09. So a soft, grippy tire
     * (slime) loses the most rolling and a hard one (iron) coasts: grip against coasting, a trade
     * nobody placed.
     */
    public static final float ROLLING_SOFT = 0.15f;
    public static final float ROLLING_HARDNESS = 0.5f;
    /**
     * Placeholder. Ground that falls in vanilla (sand, gravel) is loose whatever its grains'
     * hardness -- sand's 40 is quartz, not a road -- and gives this much more.
     */
    public static final float ROLLING_LOOSE = 0.15f;
    /**
     * Placeholder. Ground that slows a walker in vanilla (soul sand, honey: speed factor 0.4) is
     * ground a wheel sinks into: this times {@code 1 / speedFactor - 1}, about 0.45 there. Real
     * quicksand, not the film kind -- nothing drowns, a weak engine just does not climb out.
     */
    public static final float ROLLING_SINK = 0.3f;
    /**
     * Placeholder. A side whose material's sliding friction is under this loses rolling in
     * proportion: Minecraft's ice is near frictionless (a boat on blue ice is the fastest thing on
     * the ground), so a contact there wastes next to nothing. Blue ice (0.01) is the best coaster
     * the natural world has, as tire or as road -- and, gripping nothing, the worst to drive on.
     * Grippier than this is capped, not penalised again: softness already charges slime.
     */
    public static final float ROLLING_SLICK = 0.5f;
    /** Ground no material claims rolls like firm ground. */
    public static final float ROLLING_UNKNOWN_GROUND = 0.01f;
    /**
     * Real. Half the density of air, in kpg per block^3 (1.225 kg/m^3 over 2, a block a cubic metre
     * and a kpg a tonne): air drag is this times {@code Vdrag} times speed squared. Nothing invented
     * stands between the silhouette the player welded and the force it costs.
     */
    public static final float AIR_HALF_DENSITY_KPG_PER_BLOCK3 = 0.0006125f;

    // --- hazards: hot contact, scalding, shock ------------------------------------------------

    /**
     * Invented. Three named bands for what a hot thing does to bare skin, anchored on figures the
     * mod already has: water boils (pain), wood chars at the organics' oxidation point (burn), and
     * the first glow of the eye scale (sear). Named bands, never a damage-per-Tu dial -- philosophy
     * 3 -- so a player learns "that is a sear" and not a slope.
     */
    public static final Tu CONTACT_PAIN_TU = new Tu(100f);
    public static final Tu CONTACT_BURN_TU = new Tu(300f);
    public static final Tu CONTACT_SEAR_TU = new Tu(700f);

    /** Invented. Damage a band does per application, in hearts' halves. */
    public static final float CONTACT_PAIN_DAMAGE = 1f, CONTACT_BURN_DAMAGE = 2f, CONTACT_SEAR_DAMAGE = 4f;

    /** Invented. Seconds of fire a burn and a sear set, a scald sets none. */
    public static final int CONTACT_BURN_IGNITE_SECONDS = 2, CONTACT_SEAR_IGNITE_SECONDS = 4;

    /** Invented. Twice a second, as the bare wire's shock already is: a hazard and not a death trap. */
    public static final int CONTACT_INTERVAL_TICKS = 10;

    /**
     * Invented. How much of a piece's excess over the room a hand actually feels, through a
     * glove (thick, held in the palm) and through a pair of tongs (a forearm of arm and a grip
     * between the jaws and the hand). A thing is safe to hold while {@code ambient + exposure x
     * (T - ambient)} stays under what the gripping material can stand, so the arithmetic is the
     * material's own figure and not a number printed on the item.
     */
    public static final float GLOVE_EXPOSURE = 0.25f, TONGS_EXPOSURE = 0.1f;
    /** Invented. The share of a glove's exposure that is left when an insulating lining sits against the hand (a gauntlet's lining role). */
    public static final float GLOVE_LINING_SHARE = 0.5f;

    /**
     * Invented. A grip insulates when it conducts heat worse than this, on the same scale as a
     * material's {@code conductivity} (copper is {@link #HEAT_EXCHANGER_CONDUCTANCE}'s 0.1, wood
     * and leather are 3.75e-5). A property, not an identity: a metal grip simply is not one.
     */
    public static final float GRIP_MAX_CONDUCTIVITY = 0.001f;

    /**
     * Invented. A shield as a thermal sink (docs/spec_shield_thermal.md). A point of blocked damage
     * is this much Work ({@code SHIELD_WORK_PER_DAMAGE}); this share of it stays in the face
     * ({@code SHIELD_HEAT_FRACTION}); the face's thermal mass is its baked mass times
     * {@code SHIELD_THERMAL_MASS_PER_MU} (the gun's figure, for the same lack of a specific heat);
     * it sheds {@code SHIELD_COOLING_WORK_PER_TICK} of Work a tick over that mass, so a heavy face
     * cools slowly. Tuned so a six-point blow warms a steel-faced shield a couple of Tu and a thin
     * wooden one ten times that.
     */
    public static final float SHIELD_WORK_PER_DAMAGE = 40f, SHIELD_HEAT_FRACTION = 0.5f,
            SHIELD_THERMAL_MASS_PER_MU = 12f, SHIELD_COOLING_WORK_PER_TICK = 9f;

    /**
     * Invented. Capacity holds in full up to this share of the way from the room to the face's
     * softening point, falls linearly to {@code SHIELD_SOFTEN_FLOOR} of itself at it, and past it a
     * blow wears the shield {@code SHIELD_HOT_WEAR_MULT} times as hard.
     */
    public static final float SHIELD_SOFTEN_ONSET = 0.5f, SHIELD_SOFTEN_FLOOR = 0.25f, SHIELD_HOT_WEAR_MULT = 2f;

    /** Invented. Ticks a fire's contact draws a face towards its temperature per blow or fire tick (fire damage lands about every 10). */
    public static final int SHIELD_FIRE_TICKS = 10;

    /** Invented. The share of a shield face's excess over the room the hand feels through its strap ({@link #GLOVE_EXPOSURE}'s way of saying it). */
    public static final float SHIELD_GRIP_EXPOSURE = 0.25f;

    /** Invented. Volts at or above which a bare touch is a shock to be reckoned with: MV. */
    public static final float SHOCK_MIN_VOLTS = ELECTRIC_MV_VOLTS;

    /**
     * Invented. Damage per shock by band, LV up. LV keeps {@link #BARE_WIRE_SHOCK_DAMAGE}: the
     * wire's old single figure is the bottom band rather than a second rule.
     */
    public static final float SHOCK_MV_DAMAGE = 2f, SHOCK_HV_DAMAGE = 5f, SHOCK_UHV_DAMAGE = 10f;

    /**
     * Invented. An insulating glove stops a shock up to this many volts and no further: HV's
     * reference. Past it the glove is a thing that holds a charge, not one that stops it.
     */
    public static final float GLOVE_SHOCK_PROOF_VOLTS = ELECTRIC_HV_VOLTS;

    // --- the electrified fence (docs/spec_electrified_fence.md) --------------------------------

    /** Invented. Ticks before the same entity can be shocked by a fence again. */
    public static final int FENCE_SHOCK_COOLDOWN_TICKS = 10;

    /** Invented. Amps one shock pulls from its run for the tick. */
    public static final float FENCE_HIT_AMPS = 1f;

    /** Invented. Ceiling of {@code Exposure}'s multiplier on a shock. */
    public static final float FENCE_EXPOSURE_MAX = 2f;

    /** Invented. Wet (in water or rain) multiplies a shock by this. */
    public static final float FENCE_WET_FACTOR = 1.5f;

    /** Invented. Worn armour: all four slots conductive adds this, all four insulating takes it off. */
    public static final float FENCE_ARMOUR_GAIN = 0.5f, FENCE_ARMOUR_LOSS = 0.5f;

    /** Invented. Standing on a conductive floor, an insulating floor, or on nothing at all. */
    public static final float FENCE_GROUND_CONDUCTIVE = 1.25f, FENCE_GROUND_INSULATING = 0.5f,
            FENCE_AIRBORNE_FACTOR = 0.5f;

    /** Invented. Electrical conductivity (copper 1) at or above which a body counts as conducting. */
    public static final float FENCE_CONDUCTIVE_MIN = 0.1f;

    /** Invented. Horizontal knockback strength of a fence shock. */
    public static final float FENCE_KNOCKBACK = 0.5f;

    // --- the jar baton (wiki/Jar_Baton.md) -------------------------------------------------------

    /** Placeholder. Volts a baton's jar is rated for; charge above this, or a wet holder, shocks the holder. */
    public static final float BATON_RATING_VOLTS = ELECTRIC_HV_VOLTS;

    /** Placeholder. Stagger ticks per volt discharged, capped at {@link #BATON_STAGGER_MAX_TICKS}. */
    public static final float BATON_STAGGER_TICKS_PER_VOLT = 2f;
    public static final int BATON_STAGGER_MAX_TICKS = 60;

    /** Placeholder. Fraction of speed a discharged-into target loses while staggered. */
    public static final double BATON_STAGGER_SLOW = 0.5;

    // --- the boiler's pressure ---------------------------------------------------------------

    /**
     * Invented. mB of steam that read as 1 Pu in a boiler. The vessel's air is {@link
     * #PRESSURE_VESSEL_VOLUME} mB per Pu and a boiler is the same idea, so a full 4000 mB tank is
     * 4 Pu: under {@link #PRESSURE_VESSEL_DANGER_PU} on its own, which is the point -- a boiler
     * that merely sits full is not yet in danger. It is danger once it is also hot.
     */
    public static final float BOILER_STEAM_MB_PER_PU = 1000f;

    /**
     * Invented. Pu added per Tu the boiler sits over {@link #BOILER_WORKING_TU}, but only while
     * its steam tank is full -- steam with nowhere to go. A boiler making steam into an open line
     * is cooled by the work of boiling; a shut one has no such outlet, so the heat has to go
     * somewhere and is paid as pressure. This is {@code feedback_philosophy.md} 6's Hazard tier
     * and GregTech's boiler explosion, reached by the one mistake that causes it: a restricted
     * line.
     */
    public static final float BOILER_PU_PER_TU_OVER = 0.004f;

    /**
     * Invented. Above this many Pu a boiler with a relief valve fitted vents steam. Under
     * {@link #BOILER_SEEP_PU} so the valve lifts before the seams weep; relief only, it never
     * vents below this and never regulates (ruling 2026-10-13).
     */
    public static final Pu BOILER_RELIEF_PU = new Pu(3.0f);

    /**
     * Invented. mB a tick a lifted relief valve vents per Pu over {@link #BOILER_RELIEF_PU}.
     * Enough to out-run a full tank's worth of fire at a modest over-temperature, not a huge one.
     */
    public static final float BOILER_RELIEF_MB_PER_PU = 12f;

    /** Invented. Above this many Pu a boiler's seams weep: it loses steam and scalds what is beside it. */
    public static final Pu BOILER_SEEP_PU = new Pu(3.5f);

    /** Invented. mB a tick a weeping boiler loses per Pu over {@link #BOILER_SEEP_PU}. */
    public static final float BOILER_SEEP_MB_PER_PU = 6f;

    /** Invented. Blocks around a weeping or bursting boiler that its steam reaches. */
    public static final float STEAM_SCALD_REACH = 1.6f;
    public static final float STEAM_BURST_REACH = 4f;

    /** Invented. Damage of a scald per application; a burst is a sear's worth and a half. */
    public static final float STEAM_SCALD_DAMAGE = 2f;
    public static final float STEAM_BURST_DAMAGE = 8f;

    /** Invented. Explosion power of a boiler that lets go, the Pressure Vessel's own figure. */
    public static final float BOILER_BURST_POWER = 2.5f;

    // --- offline catch-up --------------------------------------------------------------------

    /**
     * Invented. A block back from an unloaded chunk is brought up to date only if it was gone at
     * least this many ticks. A chunk that flickered out and in is not worth the arithmetic.
     */
    public static final int CATCHUP_MIN_TICKS = 20;

    /**
     * Ticks a Driver's Seat key takes to ramp from nothing to full, and back (spec section 5,
     * <em>Keyboard to pedal</em>: "over about a second").
     */
    public static final int SEAT_RAMP_TICKS = 20;

    /**
     * Invented. The share of its torque and speed a running engine keeps with the throttle shut:
     * it idles, so the shaft -- and a controller turning on it -- does not stop at a red light.
     */
    public static final float ENGINE_IDLE_FRACTION = 0.15f;

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
     * Invented. How hard a brake's pads clamp at full brake, as a torque before the pad's
     * friction: {@code brake x friction x this} is what the brake holds at the hub. With an iron
     * pad (0.6) about 1.5 kN m, which locks a wheel under a quarter of a tonne -- so a full brake
     * skids and half a brake does not, and the pad material decides where between.
     */
    public static final Su BRAKE_CLAMP_SU = new Su(2500f);

    /**
     * The slider spring's travel either side of rest, in blocks (vehicles spec, differential, slider
     * and transmission). Placeholder.
     */
    public static final float SPRING_TRAVEL = 0.25f;
    /**
     * The transmission (vehicles spec). Placeholders. A gear costs {@code TRANSMISSION_INGOTS_PER_GEAR}
     * ingots and {@code TRANSMISSION_METAL_PER_GEAR_MB} of metal in the block's mass. Shifting between
     * two gears above {@code TRANSMISSION_FREE_SHIFT_RPM} wears it by speed times
     * {@code TRANSMISSION_WEAR_PER_RPM} over the material's hardness, down to
     * {@code TRANSMISSION_CONDITION_FLOOR}. Worn below {@code TRANSMISSION_POP_BELOW} it jumps out of
     * gear under load, per tick by the shortfall times {@code TRANSMISSION_POP_RATE}.
     */
    public static final int TRANSMISSION_INGOTS_PER_GEAR = 1;
    public static final float TRANSMISSION_METAL_PER_GEAR_MB = 144f, TRANSMISSION_FREE_SHIFT_RPM = 3f,
            TRANSMISSION_WEAR_PER_RPM = 0.03f, TRANSMISSION_CONDITION_FLOOR = 0.3f,
            TRANSMISSION_POP_BELOW = 0.6f, TRANSMISSION_POP_RATE = 0.01f;

    /** A motor's winding past its voltage band (user ruling, 2026-10-23): the electromagnet's rule, placeholders. */
    public static final float MOTOR_CONDITION_FLOOR = 0.5f, MOTOR_WEAR_PER_EXCESS = 0.0005f, MOTOR_HEAT_PER_EXCESS = 1f;
    public static final ThermalMass MOTOR_MASS = new ThermalMass(12f);
    public static final Conductance MOTOR_LEAK = new Conductance(0.04f);

    /**
     * Placeholder (basic electronics phase 2, mechanics 6.18). Sparks: a break of at least this
     * many amps makes one; it carries this far (a game event's radius is fixed per type).
     */
    public static final float SPARK_MIN_AMPS = 1f;
    public static final int SPARK_RANGE_BLOCKS = 16;

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

    /** Placeholder. A hot item on the ground lights a flammable neighbour once its stamp is at least this, checked every this many ticks. */
    public static final float HOT_IGNITE_TU = 300f;
    public static final int HOT_IGNITE_INTERVAL_TICKS = 10;

    // --- basic electronics phase 3 (mechanics 6.20): every figure below is a placeholder ------------

    /** Placeholder. Solder: a joint's good window above liquidus is this over the alloy's molten viscosity (a thinner melt wets better); ticks above liquidus a joint needs. */
    public static final float SOLDER_WINDOW_NUMERATOR_TU = 120f;
    public static final int SOLDER_DWELL_TICKS = 20;
    /** Placeholder. A printed trace lifts above this (its binder burns); an etched one at its metal's service temperature, else this. */
    public static final float PRINTED_TRACE_LIMIT_TU = 260f;
    /** Placeholder. What a trace route will carry once a circuit uses it (phase 4 reads these); printed is signal only. */
    public static final float ETCHED_RATING_AMPS = 2f;
    public static final float PRINTED_RATING_AMPS = 0.1f;

    /** Placeholder. Hand iron: slowest cooling rate (Tu/t) of the iron, heat each held tick draws into the joint, hottest a fire may leave it. */
    public static final TuRate IRON_COOLING_TU_PER_TICK = new TuRate(0.3f);
    public static final float IRON_JOINT_DRAW_TU = 0.6f;
    public static final float IRON_MAX_TU = 450f;

    /** Placeholder (user's numbers). Hand soldering by drag (mechanics 6.22): pad pitch on screen (px), fraction of the gap a pad closes on the iron each contact tick, ticks on a pad it needs to count as worked, lateral offset (in pitches) past which solder bridges, ticks a pad may be worked per sample, samples the server accepts, ticks leaving the screen with the button held counts as lingering, and noise a cold or bridged pad adds to a circuit. */
    public static final int SOLDER_PAD_PITCH_PX = 26;
    public static final float SOLDER_PAD_HEAT_FRACTION = 0.08f;
    public static final int SOLDER_PAD_MIN_DWELL_TICKS = 4;
    public static final float SOLDER_BRIDGE_OFFSET_PITCHES = 0.5f;
    public static final int SOLDER_PAD_MAX_DWELL_TICKS = 200;
    public static final int SOLDER_MAX_PADS = 64;
    public static final int SOLDER_LEAVE_LINGER_TICKS = 40;
    public static final float CIRCUIT_DEFECT_NOISE = 0.05f;

    /** Placeholder. Reflow oven: thermal mass and leak of the empty chamber, mass one board adds, boards held, fraction of the gap a board closes on the oven each tick. */
    public static final ThermalMass REFLOW_OVEN_MASS = new ThermalMass(40f);
    public static final Conductance REFLOW_OVEN_LEAK = new Conductance(0.05f);
    public static final float REFLOW_BOARD_MASS = 6f;
    public static final int REFLOW_SLOTS = 4;
    public static final float REFLOW_BOARD_GRIP = 0.05f;

    /** Placeholder. Fumes: radius in blocks around hot toxic solder, ticks of poison per level of the toxic perk, ticks between doses. */
    public static final float FUME_RADIUS_BLOCKS = 3f;
    public static final int FUME_POISON_TICKS_PER_LEVEL = 100;
    public static final int FUME_INTERVAL_TICKS = 40;

    /** Placeholder. Component tester: finest step it tells apart on a 0-1 figure (response, condition). */
    public static final float TESTER_RESOLUTION = 0.05f;

    /** Placeholder. Phosphor envelope: Tu below which pinching off seals nothing worth having, gas constant offset (Tu to absolute), full scale of a gauge in Pu per atmosphere. */
    public static final float ENVELOPE_SEAL_MIN_TU = 150f;
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
    // --- basic electronics phase 4 (mechanics 6.21): every figure below is a placeholder ------------

    /** Placeholder. Circuit: parts one board carries; a piezo plate rings as a clock only at or above this coupling. */
    public static final int CIRCUIT_MAX_PARTS = 4;
    public static final float CIRCUIT_CLOCK_MIN_PIEZO = 0.5f;
    /** Placeholder. Switching speed (0-1) of a crystal at rectifying 1 (scaled by its figure), of a relay, of a coherer. */
    public static final float CIRCUIT_CRYSTAL_SPEED = 1f;
    public static final float CIRCUIT_RELAY_SPEED = 0.3f;
    public static final float CIRCUIT_COHERER_SPEED = 0.1f;
    /** Placeholder. Noise (0-1) one element adds: a crystal this much plus (1 - rectifying) of {@link #CIRCUIT_CRYSTAL_POOR_NOISE}; a relay; a coherer. */
    public static final float CIRCUIT_CRYSTAL_NOISE = 0.1f;
    public static final float CIRCUIT_CRYSTAL_POOR_NOISE = 0.3f;
    public static final float CIRCUIT_RELAY_NOISE = 0.15f;
    public static final float CIRCUIT_COHERER_NOISE = 0.4f;
    /** Placeholder. Noise the trace route adds (printed is granular), and a cold joint (intermittent). */
    public static final float CIRCUIT_ETCHED_NOISE = 0.05f;
    public static final float CIRCUIT_PRINTED_NOISE = 0.2f;
    public static final float CIRCUIT_COLD_JOINT_NOISE = 0.4f;
    /** Placeholder. A clock cuts the noise by up to this share, times its piezo coupling. */
    public static final float CIRCUIT_CLOCK_STEADY = 0.5f;
    /** Placeholder. The irreducible noise floor of any circuit (digest 8: better narrows, never collapses). */
    public static final float CIRCUIT_NOISE_FLOOR = 0.03f;
    /** Placeholder. Over its trace rating an element fuses with this chance per tick. */
    public static final float CIRCUIT_FUSE_CHANCE = 0.1f;
    /** Placeholder. Whisker stage: extra reach per element, share of the hold a full-speed circuit cuts, base spread of the hunt band. */
    public static final float WHISKER_STAGE_GAIN = 0.15f;
    public static final float WHISKER_STAGE_SNAP = 0.5f;
    public static final float WHISKER_BAND_SPREAD = 0.2f;

    // --- radiation (mechanics 9.8): every figure below is a placeholder ----------------------------

    /** Placeholder. How far a block or a dropped item still counts toward a body's dose rate; a source gives 1 / (1 + d^2) of its figure. */
    public static final int RAD_REACH_BLOCKS = 5;
    /** Placeholder. Seconds between a player's dose being worked out (the server's tick; a Geiger counter reads its own 5 times a second). */
    public static final int RAD_INTERVAL_TICKS = 20;
    /** Placeholder. Dose (Ru x s) a body clears each second: a source under this in Ru does nothing however long it stays. */
    public static final float RAD_CLEARANCE = 0.2f;
    /** Placeholder. Dose at which the body complains: hunger; weakness and mining fatigue; the wither. */
    public static final float RAD_SICK_DOSE = 300f, RAD_ILL_DOSE = 900f, RAD_LETHAL_DOSE = 2400f;
    /** Placeholder. How hard worn armour shields: dose rate over (1 + this x the four slots' mean density / 10), a slot bare counting nothing. */
    public static final float RAD_SHIELD_GAIN = 6f;
    /** Placeholder. Shielding by density (density / 10 times this, then 1 / (1 + that)): per block of a wall along the line, and for the lining of a container. */
    public static final float RAD_WALL_GAIN = 1.5f, RAD_CONTAINER_GAIN = 30f;
    /** Placeholder. The step a dosimeter prints dose to (Ru x s). */
    public static final float DOSIMETER_RESOLUTION = 10f;
    /** Placeholder. A Geiger counter: clicks per second with nothing near, and per Ru; the resolution it prints to. */
    public static final float GEIGER_BACKGROUND_CPS = 0.4f, GEIGER_CPS_PER_RU = 2f, GEIGER_RESOLUTION = 0.1f;
    /** Placeholder. Wither ticks a radiant strike gives per level, and glow ticks. */
    public static final int RADIANT_WITHER_TICKS_PER_LEVEL = 60, RADIANT_GLOW_TICKS = 100;

    // --- petrochemistry, phase 1 (spec_petrochemistry): every figure below is invented ----------------

    /** Invented. A pipe run: mB each pipe segment adds to the run's ledger, and the most mB a tick a run hands to what it touches. */
    public static final int PIPE_VOLUME_MB = 500, PIPE_FLOW_MB_PER_TICK = 200;

    /** Invented. The casting basin: a block's worth of pool, the share of the gap to ambient it cools each tick, and mB it takes from a vessel each tick. */
    public static final int BASIN_POOL_MB = 1296, BASIN_POUR_MB_PER_TICK = 18;
    public static final float BASIN_COOL_PER_TICK = 0.002f;

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

    /** Invented. Vapour cloud: ticks between steps, the share of a source's potency it emits per step, the cap and floor of a cell, the share handed to neighbours, the decay per step. */
    public static final int VAPOUR_INTERVAL_TICKS = 10;
    public static final float VAPOUR_EMIT = 0.3f, VAPOUR_CAP = 1f, VAPOUR_FLOOR = 0.02f, VAPOUR_SPREAD = 0.5f, VAPOUR_DECAY = 0.9f;
    /** Invented. A flash fire: seconds a victim burns and damage at full concentration. */
    public static final int VAPOUR_FLASH_FIRE_SECONDS = 4;
    public static final float VAPOUR_FLASH_DAMAGE = 6f;

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
}
