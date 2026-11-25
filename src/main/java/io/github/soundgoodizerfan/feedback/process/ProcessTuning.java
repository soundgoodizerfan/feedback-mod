// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Qu;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.machine.MachineTuning;

/**
 * Tuning constants for the process code.
 * Every figure is a placeholder or invented unless its comment says it comes from the slice design.
 */
public final class ProcessTuning {

    private ProcessTuning() {
    }

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

    /** Invented. The share of a heat source's excess over the room that reaches a crop through one block of soil (a hearth under a planter). */
    public static final float CROP_HEAT_SHARE = 0.03f;

    /** Invented. Water leaves a food above this, in Tu, faster the hotter. */
    public static final Tu FOOD_EVAPORATION_TU = new Tu(100f);

    /** Invented. Fraction of moisture lost per tick, per Tu above {@link #FOOD_EVAPORATION_TU}. */
    public static final float FOOD_DRYING_PER_TU_TICK = 2.5e-6f;

    /** Invented. Dried means below this fraction of the moisture the food started with. */
    public static final float FOOD_DRIED_FRACTION = 0.6f;

    /** Invented. The slowest and fastest a pan or pot of any material swings, per tick. */
    public static final float COOKWARE_RELAX_MIN = 1f / 200f;

    public static final float COOKWARE_RELAX_MAX = 1f / 8f;

    /** The cookware material the swing is scaled against: its heat conductivity and density are 1. */
    public static final float COOKWARE_REF_CONDUCTIVITY = 0.016f;

    public static final float COOKWARE_REF_DENSITY = 7.8f;

    /** Invented. Heat kills this fraction of a food's live pathogen per tick at full cooking speed. */
    public static final float FOOD_KILL_PER_TICK = 0.03f;

    /** Invented. Spoilage (food spec 8): ticks for a wet food to rot through at ambient (2 h), the pathogen rot raises it to, the rot past which it turns toxic, the toxin it reaches, the rot at which it reads as going off, and the share of hunger rot takes. */
    public static final float FOOD_ROT_TICKS = 144000f;

    public static final float FOOD_ROT_TOXIC_AT = 0.7f;

    public static final float FOOD_ROT_TOXIN = 0.8f;

    public static final float FOOD_ROT_GOING_OFF = 0.3f;

    public static final float FOOD_ROT_NUTRITION_LOSS = 0.5f;

    /** Invented. Smoke (food spec 8): the coat a figure-1 smoker lays down per tick on food cooking in it, the coat lost per tick (about 17 min from full), and the share of the rot rate a full coat removes. */
    public static final float SMOKE_DEPOSIT_PER_TICK = 0.0006f;

    public static final float SMOKE_DECAY_PER_TICK = 1f / 20000f;

    public static final float SMOKE_ROT_SHIELD = 0.6f;

    /** Invented. Salt (food spec 8): the share of the rot rate full salting removes, how much more hazard a full salting tolerates, the moisture one salting draws out, and the mB of brine boiled dry per salt item (only by the sea). */
    public static final float SALT_ROT_SHIELD = 0.8f;

    public static final float SALT_TOLERANCE = 1.5f;

    public static final float SALT_DRAW = 0.15f;

    /** Invented. A fresh food is stamped to the nearest minute, so stacks made close together still merge. */
    public static final long FOOD_STAMP_BUCKET = 1200L;

    /** Invented. Two stacks of one food whose every figure (cook, moisture, pathogen, toxin, char, rot, smoke, salt) is within this of the other count as one food when a process finishes beside them, and stack. */
    public static final float FOOD_MERGE_TOLERANCE = 0.05f;

    /** Invented. Food whose last surroundings were at or under this is frozen solid until it has sat warmer this long; a frozen bite costs this much cold damage. */
    public static final Tu FROZEN_AT = new Tu(0f);

    public static final long FROZEN_THAW_TICKS = 1200L;

    public static final float FROZEN_BITE_DAMAGE = 1f;

    /**
     * Placeholder. Food actives are authored and shown in {@link Qu} (an amount per item; a pot or
     * dish carries it as Qu/mB). The effect arithmetic still works on a normalised strength, 0 to
     * 1, and this is the one conversion: strength = Qu / this. A fully loaded old 1.0 is 0.1 Qu,
     * a tenth of a nugget. Converted once at the data codec ({@code Material.Food}) and once for
     * display ({@code Material.Food.qu}).
     */
    public static final float FOOD_QU_PER_STRENGTH = 0.1f;

    /** Invented. Buffs (food spec 7): ticks of delay per unit of fat plus fibre in a food, the least strength of a compound that does anything, the fat plus fibre above which a dose comes on too slowly to crash. */
    public static final float FOOD_BUFF_DELAY_TICKS = 600f;

    public static final float FOOD_BUFF_MIN_STRENGTH = 0.05f;

    public static final float FOOD_BUFF_CRASH_ABSORB = 0.3f;

    /** Invented. Eating a warm dish (above the warm point) comforts: ticks of Regeneration, and the cold it carries is shed. */
    public static final int WARM_MEAL_REGEN_TICKS = 80;

    /** Invented. Food-perk traits, per twice-a-second pass at strength 1: freeze ticks shed (powder snow adds 1 a tick, so 12 outruns it), fire ticks shed extra, air ticks added (10 is neutral). */
    public static final int PERK_FREEZE_RELIEF_TICKS = 12;

    public static final int PERK_COOL_BURN_TICKS = 30;

    public static final int PERK_AIR_TICKS = 10;

    /** Invented. Natural-regeneration perks work only at or above this food level, the vanilla fed line. */
    public static final int PERK_FED_FOOD = 18;

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
     * Invented. The bite (dish spec 5, the zero-tier instrument): a nibble applies the food's hazard effects with chance and
     * duration times this share, and can be taken again after the cooldown.
     */
    public static final float BITE_SHARE = 0.15f;

    /** Invented. A sneak-press released within this many ticks of the eating starting is a nibble, not a meal. */
    public static final int BITE_TAP_TICKS = 6;

    /** Invented. Ticks before a bite can be taken again after one ends. */
    public static final int BITE_COOLDOWN_TICKS = 20;

    /** Invented. Ticks the bite indicator stays on the HUD. */
    public static final int BITE_HUD_TICKS = 60;

    /**
     * Invented. Seal and spill (dish spec 2.2): a container's seal 0 open, 1 covered, 2 sealed takes this much off the scale of
     * drying, migration and spill per level (covered halves them, sealed stops them), and sealed stops rot. An overfull dish
     * loses this share of its spillable overflow (mB over the container's, in parts of {@link FTuning#DISH_PART_MB}) from its wettest part each
     * time it is picked up or dropped, until it is down to what the container holds.
     */
    public static final float SEAL_SCALE_PER_LEVEL = 0.5f;

    /**
     * Invented. Prep Table work (dish spec 2.2, 4). A cut food (level 1 sliced, 2 minced) rots, cooks and dries, and gives up
     * its compounds, faster by these per level; a ragged cut leaves a thick middle that heat does not clear below the
     * pathogen floor; the evenness grades by the spread of piece lengths (coefficient of variation).
     */
    public static final float CUT_ROT_PER_LEVEL = 0.5f;

    public static final float CUT_HEAT_PER_LEVEL = 0.3f;

    public static final float KNEAD_GLOSSY_SCALE = 1.15f, KNEAD_GOOD_SCALE = 1.0f, KNEAD_CRUMBLY_SCALE = 0.8f, KNEAD_TOUGH_SCALE = 0.7f;

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
     * Invented placeholder (ore processing spec 2.4). The least share of its authored hold a form-agnostic row asks of the
     * finest form: effective hold is {@code required * clamp(grain^k, FORM_HOLD_FLOOR, 1)}, so a dust never finishes in zero
     * ticks; it still needs the band and the fuel.
     */
    public static final float FORM_HOLD_FLOOR = 0.15f;

    /**
     * Invented placeholder. Exponent {@code k} of the hold formula, one for every row: 1 is a surface-limited reaction
     * (rate goes with area). A per-row exponent for diffusion-limited rows is not built.
     */
    public static final float FORM_HOLD_EXPONENT = 1f;

    /**
     * Invented placeholder. Hold (TPu) of a derived melt row for one ingot's volume of a form of grain 1; a form of another volume
     * holds in proportion, a finer form shorter (the same grain formula as any row). A stack melts one piece at a time, so this is
     * also the ticks-per-ingot of a crucible at the optimum.
     */
    public static final float MELT_HOLD_TPU = 10f;

    /**
     * Invented placeholder. Share of a metal dust's melt lost to oxide per unit of fineness ({@code 1/grain - 1}) when it melts in
     * air above its {@code oxidation_temperature}; sealed (airless) vessels lose none. Dust of grain 0.05 (fineness 19) loses 19 percent.
     */
    public static final float DUST_BURNOFF_PER_FINENESS = 0.01f;

    /** Invented placeholder. Most of a melt any dust can burn off. */
    public static final float DUST_BURNOFF_MAX = 0.5f;

    /**
     * Invented placeholder. Share of its material's own fume level a toxic dust throws up per unit of fineness when a vessel
     * finishes a batch of it open to the air, capped at the full level.
     */
    public static final float DUST_FUME_PER_FINENESS = 0.05f;

    /**
     * Invented. Where a {@code minecraft:smoking}-classified vanilla-fallback recipe accumulates
     * TPu fastest, in Tu -- the Smoking thermal hint: food-like, prefers
     * lower and more stable heat. Sits well inside {@link #FOOD_MAX_TU}'s wall so a hold near
     * the optimum has real margin before the recipe burns instead of finishing.
     */
    public static final Tu SMOKING_OPTIMAL_TU = new Tu(170f);

    /**
     * Invented. Where a generic (non-blasting) metal-classified vanilla-fallback recipe -- the
     * {@code smelting} hint -- accumulates TPu fastest, in Tu. Set just above {@link
     * #METAL_MIN_TU} rather than deep into blasting territory: the Furnace
     * wants to be the generalist "capable of both bands, optimized for neither", and this is
     * the metal half of that mediocrity.
     */
    public static final Tu SMELTING_OPTIMAL_TU = new Tu(1000f);

    /**
     * Invented. Where a {@code minecraft:blasting}-classified vanilla-fallback recipe accumulates
     * TPu fastest, in Tu -- the Blasting hint: metallurgical, rewards a
     * hotter environment. Above {@link #SMELTING_OPTIMAL_TU} on purpose: it is the whole reason a
     * hotter, better-insulated vessel (a blast furnace) ends up finishing ore faster than a plain
     * furnace without either block ever checking the other's identity -- see {@link
     * io.github.soundgoodizerfan.feedback.process.VanillaFallback} for where this is read.
     */
    public static final Tu BLASTING_OPTIMAL_TU = new Tu(1400f);

    /**
     * Placeholder. The mass probe: per cut of crystal plate, the most coat it reads
     * (the range, in Qu) and the smallest step it tells apart. A thin cut is fine and short, a thick
     * one reaches far and reads coarse. Every range sits under {@link MachineTuning#PLATING_SPALLS_ABOVE_QU}, so
     * a probe is pegged before the coat on it spalls, which is the trap.
     */
    public static final float MASS_PROBE_RANGE_COARSE_QU = 1.71f;

    public static final float MASS_PROBE_RANGE_STANDARD_QU = 1.08f;

    public static final float MASS_PROBE_RANGE_FINE_QU = 0.54f;

    public static final float MASS_PROBE_STEP_COARSE_QU = 0.09f;

    public static final float MASS_PROBE_STEP_STANDARD_QU = 0.045f;

    public static final float MASS_PROBE_STEP_FINE_QU = 0.009f;


    // --- basic electronics phase 3: every figure below is a placeholder ------------

    /** Placeholder. Solder: a joint's good window above liquidus is this over the alloy's molten viscosity (a thinner melt wets better); ticks above liquidus a joint needs. */
    public static final float SOLDER_WINDOW_NUMERATOR_TU = 120f;

    public static final int SOLDER_DWELL_TICKS = 20;

    /** Placeholder. A printed trace lifts above this (its binder burns); an etched one at its metal's service temperature, else this. */
    public static final float PRINTED_TRACE_LIMIT_TU = 260f;

    /** Placeholder. What a trace route will carry once a circuit uses it (phase 4 reads these); printed is signal only. */
    public static final float ETCHED_RATING_AMPS = 2f;

    public static final float PRINTED_RATING_AMPS = 0.1f;

    public static final float IRON_JOINT_DRAW_TU = 0.6f;

    /**
     * Placeholder. The Tu a part's pad may reach before the part itself is cooked, per kind (the board's own limit lifts traces first or last
     * depending on the substrate): a sealed tube's bulb seal gives first, a crystal plate and a lattice next, a relay and a coherer stand most.
     * One table by kind until parts carry a material to read a service temperature from.
     */
    public static final float PART_LIMIT_TUBE_TU = 280f, PART_LIMIT_CLOCK_TU = 320f, PART_LIMIT_RESONATOR_TU = 330f, PART_LIMIT_SWITCH_TU = 340f,
            PART_LIMIT_LATTICE_TU = 360f, PART_LIMIT_RELAY_TU = 380f, PART_LIMIT_COHERER_TU = 420f;

    /** Placeholder (user's numbers). Hand soldering by drag: pad pitch on screen (px), fraction of the gap a pad closes on the iron each contact tick, ticks on a pad it needs to count as worked, lateral offset (in pitches) past which solder bridges, ticks a pad may be worked per sample, samples the server accepts, ticks leaving the screen with the button held counts as lingering, and noise a cold or bridged pad adds to a circuit. */
    public static final int SOLDER_PAD_PITCH_PX = 26;

    public static final float SOLDER_PAD_HEAT_FRACTION = 0.08f;

    public static final int SOLDER_PAD_MIN_DWELL_TICKS = 4;

    public static final int SOLDER_MAX_PADS = 64;

    public static final float CIRCUIT_DEFECT_NOISE = 0.05f;

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

    /**
     * Placeholder. How much of an instrument's step a fitted circuit can take off: the step is
     * multiplied by {@code 1 - gain * (1 - noise)}. A quiet circuit (noise near the floor) roughly
     * halves it, a noisy one barely changes it, a dead one nothing. Under 1, so a circuit narrows the
     * step and never reaches the truth tier (philosophy 10).
     */
    public static final float CIRCUIT_INSTRUMENT_GAIN = 0.6f;

    /**
     * Placeholder. A circuit's clock: the program steps a second a host takes from it. No circuit steps at the base rate; speed 1 reaches the
     * most, which is one step a tick (a program never steps twice in a tick).
     */
    public static final float CIRCUIT_BASE_STEPS_PER_SECOND = 4f, CIRCUIT_MAX_STEPS_PER_SECOND = 20f;

    /**
     * Placeholder. A circuit with a tube on it reads cold until its heater has warmed it: its speed falls to this share of the working
     * figure at zero warmth, and its noise gains up to {@link #CIRCUIT_COLD_NOISE} (both linear in warmth, never a refusal).
     */
    public static final float CIRCUIT_COLD_SPEED = 0.15f, CIRCUIT_COLD_NOISE = 0.5f;

    /** Placeholder. The most of a board's trace limit a heater warms it to: past it the traces would lift, so the heater stops there and the tube runs short of working warmth. */
    public static final float CIRCUIT_HEATER_LIMIT_SHARE = 0.9f;

    // --- resonators and lattices (circuit elements of the amethyst and sculk lines); placeholders ---

    /** A resonator: stability is {@code piezo x size / (size + STABLE_HALF_MM)}; channels are {@code 1 + round(piezo x cut x SELECT_MAX)}; bandwidth is {@code piezo x electrode coupling x (1 - NARROW x cut) x min(1, WIDE_MM / size)}. */
    public static final float RESONATOR_STABLE_HALF_MM = 6f, RESONATOR_SELECT_MAX = 7f, RESONATOR_NARROW = 0.6f, RESONATOR_WIDE_MM = 8f;

    /** A resonator's character by channels: at most the first is a clock crystal, at least the second a filter, else a band crystal. */
    public static final int RESONATOR_CLOCK_MAX_CHANNELS = 2, RESONATOR_FILTER_MIN_CHANNELS = 5;

    /** A lattice: memory is {@code round(cells x (MESH_FLOOR + (1 - MESH_FLOOR) x mesh) x capacitance / REF)}; adaptation is {@code (1 - FINE x mesh) x min(1, conductance / REF)}; retention is {@code (1 - leak / LEAK_REF) x WEIGHT + (1 - WEIGHT) x frame insulation share}. */
    public static final float LATTICE_MESH_FLOOR = 0.4f, LATTICE_CAPACITANCE_REF = 250f, LATTICE_FINE = 0.7f, LATTICE_CONDUCTANCE_REF = 3f,
            LATTICE_LEAK_REF = 0.1f, LATTICE_RETENTION_WEIGHT = 0.6f;

    /** A lattice at or over this retention holds its memory with the host unpowered; under it the memory needs a standing supply. */
    public static final float LATTICE_HOLDS_AT = 0.6f;

    /** The most extra variables any circuit gives (what a lattice can add), and the noise a resonator or lattice element adds to its board's average. */
    public static final int CIRCUIT_MEMORY_MAX = 8;

    public static final float ELEMENT_NOISE = 0.1f;

    /**
     * Placeholder. How the figures reach a host. A Controller with a circuit: its step timing varies by {@code CLOCK_JITTER x (1 - stability)};
     * a sensor quantity's read noise is cut by {@code STABLE_SAMPLING x stability}; an adapting circuit remembers each sensor's reading and holds
     * it while a new one stays within {@code ADAPT_BAND x adaptation x read jitter} of it (learning it slowly, {@code ADAPT_LEARN} a read).
     * A host's powered-off time over {@code HOLD_GRACE_TICKS} clears the memory variables of a circuit that does not hold unpowered. A Data Port's
     * bandwidth is multiplied by {@code 1 + BANDWIDTH_GAIN x bandwidth}.
     */
    public static final float CIRCUIT_CLOCK_JITTER = 0.25f, CIRCUIT_STABLE_SAMPLING = 0.5f, CIRCUIT_ADAPT_BAND = 2f, CIRCUIT_ADAPT_LEARN = 0.1f,
            CIRCUIT_BANDWIDTH_GAIN = 3f;

    public static final int CIRCUIT_HOLD_GRACE_TICKS = 100;

    /**
     * Placeholder. Gain (a circuit's, at its warmth) buys two things. A faint-signal instrument (one that states a floor) reads a signal down to
     * {@code floor / (1 + FAINT_GAIN x gain)}; fainter than that it gives no reading at all. And a data link may be longer by
     * {@code 1 + WIRE_RANGE_GAIN x gain} (the larger of its two ends' gains), for the same wire at the same cost per block.
     */
    public static final float CIRCUIT_FAINT_GAIN = 4f, CIRCUIT_WIRE_RANGE_GAIN = 1f;

    // --- vacuum tubes (spec: circuits, tube family). Design only taken from PowerGrid; see THIRD-PARTY-LICENSES.md ---

    /**
     * Placeholder. Emitter physics. A cathode's work function in eV is {@code BASE + SLOPE x electrode_potential} (reactive metals give up
     * electrons easily, noble ones do not), clamped to the min and max. The temperature it needs for a standard emission is
     * {@code K_PER_EV x work function}; the hottest it may be held is {@code MELT_SHARE x its melting point} in kelvin. Emission is 1 where it
     * can be held at the needed temperature and falls as {@code exp(SLOPE_E x (1 - needed / held))} where it cannot.
     */
    public static final float TUBE_WORK_BASE = 4.4f, TUBE_WORK_PER_VOLT = 0.45f, TUBE_WORK_MIN = 2f, TUBE_WORK_MAX = 6f;

    /** Celsius to kelvin: a {@code Tu} is a degree Celsius. */
    public static final float TUBE_KELVIN = 273.15f;

    public static final float TUBE_K_PER_EV = 580f, TUBE_MELT_SHARE = 0.85f, TUBE_EMISSION_SLOPE = 20f;

    /** Placeholder. The share of the cathode's heat above ambient that shows on the bulb, so a circuit item is warmed to {@code ambient + share x (cathode - ambient)}, never under {@link #TUBE_MIN_WARM_TU}. */
    public static final float TUBE_BULB_SHARE = 0.08f, TUBE_MIN_WARM_TU = 80f;

    /** Placeholder. Heater power, Eu a tick, of an emitter at the reference length and the reference cathode temperature; it grows with the length and the square of the temperature over the reference. */
    public static final float TUBE_HEATER_EU = 0.5f, TUBE_EMITTER_REF_MM = 15f, TUBE_CATHODE_REF_K = 2600f;

    /**
     * Placeholder. Tu a tick (gross: the circuit's stack sheds {@code ThermalTuning.ITEM_COOLING_TU_PER_TICK} against it) one Eu a tick of
     * heater warms a bulb of the reference diameter by; a wider bulb warms in proportion slower.
     */
    public static final float TUBE_WARM_TU_PER_EU = 10f, TUBE_BULB_REF_MM = 30f;

    /** Placeholder. Gain (a stage's) of a tube by its grid count, none to three; times the grid pitch factor and the grid metal's conductivity share. A diode has none. */
    public static final float[] TUBE_GRID_GAIN = {0f, 1f, 1.4f, 1.8f};

    /** Placeholder. A grid pitch of 100 per cent is this share of the gain of the finest (20 per cent): gain falls linearly across the range. */
    public static final float TUBE_COARSE_GAIN = 0.4f;

    /** Placeholder. Speed before the plate: a plate of 100 per cent area costs this share of it, a grid costs this each, and a plate metal that sheds heat poorly costs up to this much of the rest. */
    public static final float TUBE_PLATE_SLOW = 0.5f, TUBE_GRID_SLOW = 0.08f, TUBE_HOT_PLATE_SLOW = 0.3f;

    /** Placeholder. Thermal conductivity (the table's scale: copper 0.1) at which a plate sheds heat fully. */
    public static final float TUBE_PLATE_COND_REF = 0.1f;

    /** Placeholder. A grid metal's conductivity (copper 1) at which the grid holds its potential fully; under it the gain falls by up to this share. */
    public static final float TUBE_GRID_COND_LOSS = 0.5f;

    /** Placeholder. Noise (0-1) of a tube: a floor, plus up to this much for a poor emitter, plus up to this much for a soft vacuum. */
    public static final float TUBE_NOISE_BASE = 0.04f, TUBE_NOISE_POOR_EMITTER = 0.4f, TUBE_NOISE_GAS = 0.6f;

    /** Placeholder. A seal under this vacuum quality holds gas: a grid tube with it is a thyratron. */
    public static final float TUBE_GAS_BELOW = 0.5f;

    /**
     * Placeholder. What each character does to the figures, by {@code Tube.Kind} ordinal (diode, triode, tetrode, pentode, thyratron): a
     * share of the speed, a share of the noise, a share of the gain.
     */
    public static final float[] TUBE_KIND_SPEED = {1f, 0.9f, 0.95f, 1f, 0.35f};

    public static final float[] TUBE_KIND_NOISE = {1f, 1f, 1.3f, 0.8f, 1.5f};

    public static final float[] TUBE_KIND_GAIN = {0f, 1f, 1f, 1f, 2.5f};

    /**
     * Placeholder. Sealing: a vacuum dryer pulls the chamber to a depth (0 at the dryer's working gauge, 1 at its deepest); the seal's
     * vacuum quality is {@code (FLOOR + (1 - FLOOR) x depth) x (BAKE_FLOOR + (1 - BAKE_FLOOR) x bake) x tightness}, where bake is how far the
     * chamber's heat has come between {@code BAKE_MIN_TU} and {@link #CIRCUIT_HEATER_LIMIT_SHARE} of the envelope's service temperature.
     * An envelope hotter than its service temperature slumps and seals no vacuum.
     */
    /** Placeholder. Envelope tightness: corrosion resistance times {@code FLOOR + (1 - FLOOR) x log10(melt viscosity) / LOG}, so a stiff glass holds a better vacuum than a runny ceramic. */
    public static final float TUBE_TIGHT_FLOOR = 0.6f, TUBE_TIGHT_VISC_LOG = 5f;

    public static final float TUBE_VACUUM_FLOOR = 0.2f, TUBE_BAKE_FLOOR = 0.5f, TUBE_BAKE_MIN_TU = 150f;

    /** mB of the quench oil a dunk in a tank costs (invented). */
    public static final int QUENCH_TANK_MB = 50;

    public static final float SMOULDER_TEMPERATURE = 0.4f, SMOULDER_DURATION = 0.5f;
}
