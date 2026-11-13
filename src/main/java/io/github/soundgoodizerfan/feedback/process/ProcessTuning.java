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
    public static final Tu SMOKING_OPTIMAL_TU = new Tu(250f);

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

    /** mB of the quench oil a dunk in a tank costs (invented). */
    public static final int QUENCH_TANK_MB = 50;

    public static final float SMOULDER_TEMPERATURE = 0.4f, SMOULDER_DURATION = 0.5f;
}
