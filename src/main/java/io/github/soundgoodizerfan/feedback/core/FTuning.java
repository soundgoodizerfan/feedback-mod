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

import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.Drag;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
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
 * Anything here marked SLICE comes from feedback_slice_01.md. Everything else is invented.
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
     * SLICE: a hand hammer swings at about 3 St.
     *
     * <p>Against copper's hardness of 1 that is 3 Fu a swing, so a 14 Fu plate is five swings and
     * the sixth starts making foil. It is deliberately the same figure as the Mechanical Hammer's
     * long throw: the gentle machine is doing exactly what the player's arm was doing, only without
     * ever getting bored, which is the whole of what beat 1 has to say.
     *
     * <p>It is also what keeps steel out of reach by hand with no rule about hands anywhere. Steel's
     * hardness is 15, and 3 St below a hardness of 15 lands nothing at all -- philosophy 7's hard
     * gate, a genuine impossibility rather than a slow route.
     */
    public static final St HAND_HAMMER_ST = new St(3f);

    /**
     * Invented. Blows a hand hammer lands before it is finished.
     *
     * <p>Sized as a working figure rather than a balance lever: 250 swings is fifty copper plates,
     * which is plenty to bootstrap a first Mechanical Hammer and nowhere near enough to want to
     * keep doing it. Philosophy 7 promises manual production stays <em>possible</em>, not that it
     * stays free.
     */
    public static final int HAND_HAMMER_DURABILITY = 250;

    public static final St HAMMER_ST_SHORT = new St(12f);
    public static final St HAMMER_ST_LONG = new St(3f);

    /** SLICE: 80 Su, against the water wheel's 256. Three hammers fit; four do not. */
    public static final Su HAMMER_LOAD_SU = new Su(80f);

    /**
     * Invented. The most St this hammer can land, however it is geared.
     *
     * <h3>Why a ceiling exists</h3>
     * Gearing down multiplies force per blow without limit, so without a ceiling one long gear
     * train makes every hammer in the game equivalent and the force axis collapses into how many
     * cogs somebody was willing to place. A paper blade at a million RPM still will not cut steel:
     * what a machine can deliver is a property of its construction, not of its drive.
     * <p>
     * Twice the short throw, so gearing buys exactly one genuine doubling past the strongest
     * setting the block has -- enough for the trade to be worth making, not enough to replace
     * buying a better machine. Raising the ceiling is what a better machine is <em>for</em>.
     */
    public static final St HAMMER_MAX_ST = new St(24f);

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

    // --- thermal ---------------------------------------------------------------------------

    /**
     * Invented. The temperature everything decays towards, in Tu.
     *
     * <h3>Why one figure and not a biome lookup</h3>
     * A desert crucible holding two degrees hotter than a taiga one is a variable the player
     * cannot act on -- they are not going to move the factory -- and philosophy 8 is explicit
     * that a variable nobody can act on is not worth modelling. One number, everywhere.
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
     *   <li><b>Not taken: the exploding boiler.</b> `feedback_philosophy.md` §14 already names
     *       this as good precedent for a genuine hard-gate consequence at the top of a tech
     *       ladder. Left out because there is no failure state to explode into yet -- this boiler
     *       cannot currently be over-restricted -- and adding one now would be inventing a
     *       punishment for a mistake the player cannot make. Worth revisiting once pressure, not
     *       just presence, of steam is modelled; see `TODO.md` §5a.</li>
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
     */
    public static final Tu SMOKER_CEILING_TU = new Tu(380f);

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
     * Invented. A Pressure Vessel's fixed internal volume, in the same air units {@link
     * io.github.soundgoodizerfan.feedback.machine.bellows.Blown#addAir} already takes -- see
     * {@code PressureVesselBlockEntity}. Pressure is air over this, so a bigger vessel (a later,
     * unbuilt upgrade) reads a lower pressure for the same bellows work, same shape as {@code
     * ThermalMass} turning Work into Tu.
     */
    public static final int PRESSURE_VESSEL_VOLUME = 1000;

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
    public static final float PRESSURE_VESSEL_DANGER_PU = 5f;

    /** Invented. At or above this many Pu, a Pressure Vessel explodes outright, every tick. */
    public static final float PRESSURE_VESSEL_CRITICAL_PU = 7f;

    /**
     * Invented. A Pressure Vessel is not perfectly sealed -- a small constant bleed, in the same
     * air units as {@link #PRESSURE_VESSEL_VOLUME}, so a vessel nobody is actively pumping settles
     * back to nothing rather than holding whatever it was last left at forever. Small enough that
     * a bellows worked at any real rate climbs faster than this drains.
     */
    public static final int PRESSURE_VESSEL_LEAK_PER_TICK = 1;

    /**
     * Invented. Ticks-equivalent of TPu lost per tick a hand-authored or vanilla-fallback
     * process spends outside its useful temperature range, per {@code tpu_spec_doc.md}'s
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
     * TPu fastest, in Tu -- {@code tpu_spec_doc.md}'s "Smoking" thermal hint: food-like, prefers
     * lower and more stable heat. Sits well inside {@link #FOOD_MAX_TU}'s wall so a hold near
     * the optimum has real margin before the recipe burns instead of finishing.
     */
    public static final Tu SMOKING_OPTIMAL_TU = new Tu(250f);

    /**
     * Invented. Where a generic (non-blasting) metal-classified vanilla-fallback recipe -- the
     * {@code smelting} hint -- accumulates TPu fastest, in Tu. Set just above {@link
     * #METAL_MIN_TU} rather than deep into blasting territory: {@code tpu_spec_doc.md}'s Furnace
     * section wants the generalist "capable of both bands, optimized for neither", and this is
     * the metal half of that mediocrity.
     */
    public static final Tu SMELTING_OPTIMAL_TU = new Tu(1000f);

    /**
     * Invented. Where a {@code minecraft:blasting}-classified vanilla-fallback recipe accumulates
     * TPu fastest, in Tu -- {@code tpu_spec_doc.md}'s "Blasting" hint: metallurgical, rewards a
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
     * Invented. How much air the firebox can hold, and how fast it consumes it.
     *
     * <h3>Why a buffer exists</h3>
     * Strokes arrive in lumps and a fire does not flare and die between them. A small buffer
     * smooths the gap; a large one would let the player bank air and blast on demand, which turns
     * a continuous actuator into a battery and removes the reason to keep the bellows running.
     * Ten ticks of full burn is enough for the first and not the second.
     */
    public static final float FIREBOX_MAX_AIR = 40f;
    public static final float FIREBOX_AIR_PER_TICK = 4f;

    // --- the bellows ------------------------------------------------------------------------

    /**
     * SLICE: 10 air on a short throw, 30 on a long one.
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
    public static final float BELLOWS_AIR_SHORT = 10f;
    public static final float BELLOWS_AIR_LONG = 30f;

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
     * {@link io.github.soundgoodizerfan.feedback.machine.linkage.CrankLinkageBlockEntity} multiplies a
     * machine's stated force by the gear advantage and then clamps to its ceiling. Setting the
     * ceiling <em>at</em> the long throw makes that clamp bite immediately, so no gear train ever
     * moves more air per stroke -- which is correct rather than a restriction. A bellows holds
     * what it holds; squeezing it harder does not find more air inside it.
     * <p>
     * So on the hammer gearing buys force, and on the bellows it buys only stroke rate. One
     * mechanism, two honest answers, because the ceiling is a property of the machine.
     */
    public static final float BELLOWS_MAX_AIR = BELLOWS_AIR_LONG;

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
    public static final float THERMOMETER_RESOLUTION_TU = 25f;

    /**
     * SLICE: a fitted temperature sensor resolves finer than the carried thermometer.
     * <p>
     * It is bolted to one vessel rather than carried everywhere, so the trade for that loss of
     * flexibility is a number a purpose-built fixture can actually hold. Still not zero -- §8's
     * noise floor applies here exactly as it does to every other instrument.
     */
    public static final float TEMPERATURE_SENSOR_RESOLUTION_TU = 10f;

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
    public static final Tu[] HEAT_BAND_TOPS = {
            new Tu(200f),    // warm
            new Tu(480f),    // hot
            new Tu(580f),    // faint red
            new Tu(730f),    // dark red
            new Tu(930f),    // bright red
            new Tu(1100f),   // orange
            new Tu(1300f),   // yellow
            new Tu(1400f),   // pale yellow
            new Tu(1500f),   // white
            MAX_VISIBLE_TU
    };


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
    public static final int[] TIMER_SETTINGS = { 40, 100, 200, 400 };

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
}
