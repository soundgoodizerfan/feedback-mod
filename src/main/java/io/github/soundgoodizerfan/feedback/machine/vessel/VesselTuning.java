// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.vessel;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.fluid.FluidTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationTuning;
import io.github.soundgoodizerfan.feedback.core.thermal.ThermalTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.Inertia;
import io.github.soundgoodizerfan.feedback.core.unit.Mb;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;
import io.github.soundgoodizerfan.feedback.core.unit.Su;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;
import io.github.soundgoodizerfan.feedback.machine.column.PetrochemTuning;
import io.github.soundgoodizerfan.feedback.process.ProcessTuning;

/**
 * Tuning constants for the machine/vessel code.
 * Every figure is a placeholder or invented unless its comment says it comes from the slice design.
 */
public final class VesselTuning {

    private VesselTuning() {
    }
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

    /**
     * Invented. The Hearth's brick pan: between the small and the large crucible, so it is slow
     * enough to hold a fire's heat through a gust of cold stock and quick enough to be warm by
     * the time the first part is shaped.
     */
    public static final ThermalMass HEARTH_MASS = new ThermalMass(60f);

    /**
     * Invented. An open pan loses heat from every face, so more than a vessel's {@link ThermalTuning#VESSEL_LEAK}.
     * Against {@link ThermalTuning#FIRE_CONDUCTANCE} (0.6) it settles at about nine tenths of its fire.
     */
    public static final Conductance HEARTH_LEAK = new Conductance(0.06f);

    /** Invented. Chance per tick, per unit of how far over its oxidation temperature (as a share of it) a piece of raw stock sits in a Hearth, that one piece scales away. */
    public static final float HEARTH_SCALE_RATE = 0.0085f;

    /** Invented. How many items a crucible holds. Two, because carburizing needs iron and carbon. */
    public static final int CRUCIBLE_SLOTS = 2;

    /** Invented. How many items a large crucible holds: rows of three, room for a charge, its flux and a mould's worth of products. */
    public static final int CRUCIBLE_LARGE_SLOTS = 6;

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

    /** Invented. How much a held item takes from a vessel's tank per dip, in mB -- a quarter of
     * {@link FTuning#MOLD_CAPACITY_MB}, because a coat is thinner than a cast. See {@code
     * process/Dip.java} and {@code machine/dip/Dipping.java}. */
    public static final int DIP_MB = 36;

    /**
     * Invented. What one block of insulation packed against a vessel multiplies its leak by, and
     * how many are counted.
     *
     * <h3>Why this is a block and not an upgrade slot</h3>
     * Philosophy 5: an upgrade is a physical component you could point at. Insulation is the most
     * literal case in the mod -- you build it around the thing, and how well it works depends on
     * how much of the thing you covered. Six faces, of which one is the fire and one the lid, so
     * four is the practical maximum and the cap is where the geometry already put it.
     */
    public static final float INSULATION_LEAK_FACTOR = 0.8f;

    public static final int INSULATION_MAX_BLOCKS = 4;

    /**
     * Invented. Extra leak each open Damper adds, additive on top of the insulated base -- a
     * vent bypasses the insulating layer rather than negating it, so it adds where insulation
     * multiplies. Same dimension as {@link ThermalTuning#VESSEL_LEAK} by construction (Work/t/Tu).
     */
    public static final Conductance DAMPER_LEAK_BONUS = new Conductance(0.05f);

    public static final int DAMPER_MAX_BLOCKS = 4;

    /**
     * Invented. Work required to raise the boiler by one Tu.
     * <p>
     * Between the two crucible masses ({@link #CRUCIBLE_SMALL_MASS}, {@link MachineTuning#CRUCIBLE_LARGE_MASS})
     * on purpose: a boiler's whole point is to be far easier to bring into its working band than
     * a smelting vessel is -- {@link MachineTuning#BOILER_WORKING_TU} sits below even {@link ProcessTuning#FOOD_MAX_TU},
     * nowhere near {@link ProcessTuning#METAL_MIN_TU} -- so it should climb like the small crucible, not the
     * large one.
     */
    public static final ThermalMass BOILER_MASS = new ThermalMass(40f);

    /**
     * Invented. mB of Steam produced per tick once the boiler clears {@link MachineTuning#BOILER_WORKING_TU},
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

    /** Invented. The share of a boiling's latent work that lifting water one Tu toward its boiling point costs (about a sixth over a 100 Tu climb, as for real water). */
    public static final float FEED_SENSIBLE_PER_TU = 0.00185f;

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

    /** Invented. The Large Boiler is ten Boilers in one: ten times the mass, steam a tick and tanks, standing on ten times the fire. */
    /** The most plain boilers one boiler block can be (the boiler shell's {@code scale} axis). */
    public static final int BOILER_LARGE_SCALE = 10;


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
     * of Steam: philosophy §8's genuine hard gate, not enough pressure, and not a precision
     * problem at all.
     */
    public static final Su STEAM_ENGINE_CAPACITY_SU = new Su(64f);

    /** Invented. Flat output speed once working -- see {@link #STEAM_ENGINE_CAPACITY_SU}'s doc
     * on why this is a threshold and not a curve. */
    public static final Rpm STEAM_ENGINE_RPM = new Rpm(16f);

    /**
     * Invented. A flywheel, and a real one -- heavier than a shaft or a cog, lighter than the
     * Water Wheel's slab of timber. See {@link RotationTuning#SHAFT_INERTIA}'s doc for how the inertia figures
     * are meant to be read against the Su figures they pair with.
     */
    public static final Inertia STEAM_ENGINE_INERTIA = new Inertia(150f);


    // --- the steam turbine ---------------------------------------------------------------

    /** Invented, placeholder. Coolest steam the turbine runs on, Tu: a bladed rotor needs hotter, drier steam than a piston. */
    public static final float STEAM_TURBINE_MIN_TU = 150f;

    /** Invented, placeholder. How much cooler the exhaust leaves than the intake, Tu: a turbine takes more of the steam's heat than a piston does. */
    public static final float STEAM_TURBINE_EXHAUST_DROP_TU = 80f;

    /** Placeholder. Output bands scaling with steam heat, the engine's logic ({@link #STEAM_ENGINE_BANDS}). */
    public static final int STEAM_TURBINE_BANDS = 4;

    /** Placeholder. Steam heat per band step above {@link #STEAM_TURBINE_MIN_TU}, Tu. */
    public static final float STEAM_TURBINE_BAND_TU = 60f;

    /** Invented. Steam drawn per tick while working, mB: ten times the engine, so about ten boilers' worth of {@link #BOILER_STEAM_PER_TICK_MB}. */
    public static final int STEAM_TURBINE_MB_PER_TICK = 100;

    /** Invented. Input and exhaust tank size, mB: ten ticks of a stutter, the engine's ratio. */
    public static final int STEAM_TURBINE_TANK_MB = 1000;

    /** Invented. Full supply capacity, Su: the large frame's rating, so at {@link #STEAM_TURBINE_RPM} it makes 64 x 240 = 15.4 kW (about 16 kW). */
    public static final Su STEAM_TURBINE_CAPACITY_SU = new Su(64f);

    /** Invented. Flat output speed once working, RPM: fifteen times the engine's. */
    public static final Rpm STEAM_TURBINE_RPM = new Rpm(240f);

    /** Invented. A heavy bladed rotor, twice the engine's flywheel. */
    public static final Inertia STEAM_TURBINE_INERTIA = new Inertia(300f);

    /** Invented. The skillet (food spec 6): ticks for the food's own heat to close most of the way (63%) to the pan's. A thick cut would lag longer; see {@code SkilletBlock#warm}. */
    public static final float SKILLET_LAG_TICKS = 60f;

    /** Invented. The skillet pan follows the fire (or the room, when lifted) by this share of the gap per tick, for a pan of the reference material (wrought iron): about a second and a half to close most of it. */
    public static final float SKILLET_PAN_RELAX = 1f / 30f;

    /** Invented. The empty pot's own thermal mass, as mB of water it weighs the same as: contents add to it, so a full pot warms slowly and cold water or food cools a hot one when added. */
    public static final float POT_EMPTY_MASS_MB = 600f;

    /** Invented. mB of water boiled off per tick, per Tu the fire is over {@link FTuning#POT_BOIL_TU}. */
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
    public static final Tu POT_SCORCH_FIRE_TU = new Tu(800f);

    public static final float POT_SCORCH_PER_TU_TICK = 5e-6f;

    public static final float POT_MIX_PER_TICK = 0.05f;

    public static final float POT_MIX_DECAY_PER_TICK = 0.004f;

    public static final float POT_SCUM_YIELD = 0.015f;

    public static final float POT_SKIM_AMOUNT = 0.25f;

    public static final float POT_FOLD_PER_TICK = 0.02f;

    public static final float POT_SCUM_HAZARD = 0.4f;

    /** Invented. A fully mixed pot cooks this much faster (heat carried through the body); an unmixed one lets the solids settle, so the first ladle of a pot of n carries this fraction less of the food than its share, and the later ones more. */
    public static final float POT_STIR_COOK_BONUS = 0.5f;

    public static final float POT_SETTLE = 0.3f;

    /** Invented. Share of solids in the mixture under which a pot is a broth, not a stew. */
    public static final float POT_BROTH_SOLIDS = 0.2f;

    /** Invented. The figure a smoker has: how much smoke it lays down against a figure-1 vessel. The furnace and blast furnace have none. */
    public static final float SMOKER_SMOKE = 1f;

    public static final float SALT_MB_PER_ITEM = 500f;

    /** Invented. The ice box: ticks one piece of ice keeps it cold, the temperature it holds while it has any, and the fraction of the way to the room it drifts per second once it has none. */
    public static final float ICEBOX_ICE_TICKS = 12000f;

    public static final float ICEBOX_DRIFT_PER_SECOND = 0.005f;

    public static final int REFRIGERATION_LAPSE_TICKS = 30;

    /**
     * Invented. The Cryocooler's cold head: the current it asks for, amps, and the three cold-head stages as the temperature the head
     * holds against what it touches. A vessel on the head settles at 0.96 of the way from ambient to the stage (the fire conductance
     * over the vessel leak): about -194, -190 and -185 Tu, which sit just either side of the boiling points of nitrogen (-196),
     * argon (-186) and oxygen (-183) so each stage is a different cut of cold air.
     */
    public static final float CRYOCOOLER_AMPS = 6f;

    public static final Tu CRYOCOOLER_DEEP_TU = new Tu(-203f), CRYOCOOLER_MID_TU = new Tu(-199f), CRYOCOOLER_SHALLOW_TU = new Tu(-193.5f);

    /**
     * Invented. Hard ceiling on a Smoker's own temperature, in Tu -- the one explicit band wall
     * in §15, because leak and mass alone do not produce one.
     *
     * <h3>Below {@link ProcessTuning#FOOD_MAX_TU}, not above it -- got backwards once already</h3>
     * The first version of this wall sat at 600: below {@link ProcessTuning#METAL_MIN_TU} (800), and — the
     * reasoning actually written down here at the time — "comfortably above {@code
     * FOOD_MAX_TU}." That was exactly backwards, and it shipped a Smoker that burned its own food
     * just as fast as a plain Furnace. The mistake was treating "cannot reach metal's floor" as
     * the whole of what a *food* appliance needs, when the real requirement is the mirror image:
     * it must never reach food's ceiling either. {@link ThermalTuning#FIRE_CONDUCTANCE} (0.6) against this
     * mass makes a lit vessel close most of the gap to its fire's temperature in well under a
     * second -- far faster than any food recipe's own cook time -- so a wall merely *below*
     * metal's floor still leaves the whole 400-800 Tu gap for the climb to blow straight through
     * on the way. The wall has to sit under {@link ProcessTuning#FOOD_MAX_TU} instead, so the vessel settles
     * there and holds, rather than crossing it in transit.
     *
     * <h3>Why this is a clamp and the Furnace gets none</h3>
     * {@link ThermalTuning#FIRE_CONDUCTANCE} dominates any leak this file could reasonably pick, so every
     * vessel's equilibrium sits close to its fire's own temperature almost regardless of leak --
     * mass changes how fast a vessel gets there and how much fuel noise it smooths out, not how
     * hot it tops out. A Smoker specialised by leak alone would still smelt ore given a hot
     * enough fuel, which is a whitelist by another name and exactly what §15 is written against.
     * So it gets a real physical wall instead. The Furnace and the Crude Blast Furnace get no
     * such clamp -- their whole difference from each other and from this is mass and leak, which
     * is the genuinely emergent half of the claim. The Smoker is the one place the mechanism is
     * declared rather than discovered, and this should say so rather than pretend otherwise.
     *
     * <h3>Under {@link FTuning#FOOD_CHAR_TU}, over {@link FTuning#FOOD_COOK_FULL_TU} -- got wrong a second time</h3>
     * 380 cleared {@link ProcessTuning#FOOD_MAX_TU}'s old reasoning but not the food rework's: food chars above
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
     * dip under {@link ProcessTuning#METAL_MIN_TU} mid-hold on a rough batch of fuel and stall rather than
     * fail outright (§6) -- and the same low mass clears {@link ProcessTuning#FOOD_MAX_TU} quickly if nobody
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
     * {@link ProcessTuning#METAL_MIN_TU} and above once it gets there -- the same claim in practice, with no
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
     * 1300; {@link ProcessTuning#METAL_MIN_TU} (800) is nowhere near it, so the wall that actually matters here
     * is steel's own floor, the steel recipe's 1350. Set a little under that -- a Kiln can
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
     * #SMOKER_CEILING_TU} sits under {@link ProcessTuning#FOOD_MAX_TU}: under the recipe's own ceiling, so the
     * vessel settles into the band rather than blowing through it on the way up (the same
     * {@link ThermalTuning#FIRE_CONDUCTANCE}-dominates-leak reasoning as the Smoker's own doc). The numeric
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
     * Invented. Puddling Furnace thermal mass and leak: a reverberatory hearth, so heavier than the Furnace and lighter than the Blast
     * Furnace (it has to climb to 1450 with the charge and never be stoked past 1500), and the Su its rabble takes off a shaft while it
     * stirs. The rabble's own speed band is on the row ({@code puddle_iron.json}), not here.
     */
    public static final ThermalMass PUDDLING_FURNACE_MASS = new ThermalMass(120f);

    public static final Conductance PUDDLING_FURNACE_LEAK = new Conductance(0.015f);

    public static final Su PUDDLING_FURNACE_LOAD_SU = new Su(60f);

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
     * of patience changes that -- philosophy 8's hard gate, a genuine impossibility rather than a
     * slow version. Air is the only way across, and air is the only thing the bellows sells.
     * <p>
     * Fully blown, charcoal reaches about 1830 Tu, which is well past the 1540 Tu at which iron
     * burns. That asymmetry is deliberate (§6): the player's one actuator points at a temperature
     * that destroys the work, so holding the window means knowing when to <em>stop</em> asking,
     * which is the entire beat.
     */
    public static final float FULL_AIR_TEMPERATURE_FACTOR = 1.5f;

    /**
     * A lit soul campfire: the Nether's reward. Clears copper, bronze, brass, gold and silver with
     * margin (a vessel only approaches its fire), stays under cast iron, lava and steel.
     */
    public static final Tu FIRE_TU_SOUL_CAMPFIRE = new Tu(1050f);

    /**
     * Invented. A campfire with no air block touching it smoulders: its fire is capped here, inside
     * charcoal's 300-500 Tu band (optimum 400) and well under wood's 700 Tu spoil, so logs buried
     * with it char instead of burning. Both campfires cap alike; the soul fire is starved too.
     */
    public static final Tu FIRE_TU_BURIED_CAMPFIRE = new Tu(420f);

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

    /** Placeholder. Tu past a part's service temperature that costs one flaw when it is dipped (up to the flaw cap). */
    public static final float DIP_FLAW_TU = 300f;

    /** Invented, grid spec 2.3. A thermal store: a brick tank of salt, thirty times the Hearth's mass, leaking slowly to air. */
    public static final ThermalMass THERMAL_STORE_MASS = new ThermalMass(1800f);

    public static final Conductance THERMAL_STORE_LEAK = new Conductance(0.05f);


    // --- the boiler's pressure ---------------------------------------------------------------

    /**
     * Invented. mB of steam that read as 1 Pu in a boiler. The vessel's air is {@link
     * PetrochemTuning#PRESSURE_VESSEL_VOLUME} mB per Pu and a boiler is the same idea, so a full 4000 mB tank is
     * 4 Pu: under {@link FluidTuning#PRESSURE_VESSEL_DANGER_PU} on its own, which is the point -- a boiler
     * that merely sits full is not yet in danger. It is danger once it is also hot.
     */
    public static final float BOILER_STEAM_MB_PER_PU = 1000f;

    /**
     * Invented. Pu added per Tu the boiler sits over {@link MachineTuning#BOILER_WORKING_TU}, but only while
     * its steam tank is full -- steam with nowhere to go. A boiler making steam into an open line
     * is cooled by the work of boiling; a shut one has no such outlet, so the heat has to go
     * somewhere and is paid as pressure. This is {@code feedback_philosophy.md} 6's Hazard tier
     * and GregTech's boiler explosion, reached by the one mistake that causes it: a restricted
     * line.
     */
    public static final float BOILER_PU_PER_TU_OVER = 0.004f;

    /**
     * Invented. mB a tick a lifted relief valve vents per Pu over {@link FTuning#BOILER_RELIEF_PU}.
     * Enough to out-run a full tank's worth of fire at a modest over-temperature, not a huge one.
     */
    public static final float BOILER_RELIEF_MB_PER_PU = 12f;

    /** Invented. Above this many Pu a boiler's seams weep: it loses steam and scalds what is beside it. */
    public static final Pu BOILER_SEEP_PU = new Pu(3.5f);

    /** Invented. mB a tick a weeping boiler loses per Pu over {@link #BOILER_SEEP_PU}. */
    public static final float BOILER_SEEP_MB_PER_PU = 6f;

    /** Placeholder. Reflow oven: thermal mass and leak of the empty chamber, mass one board adds, boards held, fraction of the gap a board closes on the oven each tick. */
    public static final ThermalMass REFLOW_OVEN_MASS = new ThermalMass(40f);

    public static final Conductance REFLOW_OVEN_LEAK = new Conductance(0.05f);

    public static final float REFLOW_BOARD_MASS = 6f;

    public static final int REFLOW_SLOTS = 4;

    public static final float REFLOW_BOARD_GRIP = 0.05f;

    public static final int FUME_INTERVAL_TICKS = 40;

    /** Invented. How much oxygen balance a fuel brings, as a share of full draught, to a firebox that burns it (spec 10.1): it carries its own oxidiser, so it burns as if blown. */
    public static final float FIREBOX_OXIDISER_DRAUGHT = 1f;

    /** Invented. A Firebox's oxygen tank (mB), the oxygen one oxygen-poor piece burns, and how long a piece lasts when saltpetre is its oxidiser (a short violent burst, as a share). */
    public static final int FIREBOX_OXYGEN_TANK_MB = 500, FIREBOX_OXYGEN_MB_PER_PIECE = 25;

    public static final float NITRE_BURST_DURATION = 0.5f;

    /** Invented. The fluid-fuel burner: what it holds (mB) and burns a tick. */
    public static final int BURNER_TANK_MB = 2000;

    public static final float BURNER_MB_PER_TICK = 0.5f;

    /** Invented. Points a vessel banks from vanilla cooking recipes before it stops banking; a Smelter Tap drains the bank a second at a time. */
    public static final float VESSEL_XP_BANK_MAX = 400f;

    /** Invented. What a hot metal bath cools by per mB that boils off it, in Tu. */
    public static final float METAL_BOIL_COOLING_TU_PER_MB = 0.05f;

    /** The hearth's tank: 36 ingots of metal. */
    public static final int ARC_TANK_MB = 5184;

    /** Invented. An arc hearth without a refractory lining wears above this bath temperature; with one, above the second figure. The wear per Tu over, per tick. */
    public static final float ARC_HEARTH_UNLINED_TU = 2200f, ARC_HEARTH_LINED_TU = 3300f;

    public static final float ARC_HEARTH_WEAR_PER_TU_EXCESS = 0.00002f;

    public static final float ARC_HEARTH_CONDITION_FLOOR = 0.5f;

    /** Invented. The share of the gap to the crucible's temperature a laid-in piece closes each tick (about 5 s to settle). */
    public static final float CRUCIBLE_SOAK_PER_TICK = 0.01f;

    /** Placeholder. The stockpot is a pot of cast iron cookware; there is no pot variant to read a material from. */
    public static final net.minecraft.resources.ResourceLocation POT_MATERIAL = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("feedback", "wrought_iron");
}
