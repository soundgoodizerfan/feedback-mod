// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import io.github.soundgoodizerfan.feedback.core.electric.ElectricTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Pu;
import io.github.soundgoodizerfan.feedback.core.unit.Qu;
import io.github.soundgoodizerfan.feedback.core.unit.St;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.TuRate;
import io.github.soundgoodizerfan.feedback.process.ProcessTuning;

/**
 * Tuning constants for the item code.
 * Every figure is a placeholder or invented unless its comment says it comes from the slice design.
 */
public final class ItemTuning {

    private ItemTuning() {
    }
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

    /** Invented. A test strip reads a food's toxin, in Qu/mB, in quarter steps of a full strength ({@link ProcessTuning#FOOD_QU_PER_STRENGTH}): four bands, never a figure it cannot stand behind. */
    public static final float TEST_STRIP_RESOLUTION_TOXIN = 0.25f * ProcessTuning.FOOD_QU_PER_STRENGTH;

    /**
     * SLICE: the crude thermometer resolves to 25 Tu.
     *
     * <h3>The number is chosen to be not quite enough</h3>
     * Steel's window is 60 Tu wide, so this splits it into two and a bit -- the player can tell
     * they are roughly in it and never exactly where. That is the point. An instrument that made
     * the window trivial would end the beat on purchase, and philosophy 10 is explicit that better
     * equipment narrows the distribution and never collapses it. A later thermometer buys
     * significant figures here and nothing else; it is one number.
     */
    public static final Tu THERMOMETER_RESOLUTION_TU = new Tu(25f);

    /** The pyrometer's step: finer than the thermometer because a lens gathers the glow onto a sensor. Invented. */
    public static final Tu PYROMETER_RESOLUTION_TU = new Tu(10f);

    /** How far a pyrometer sees a hot vessel, in blocks. Invented. */
    public static final float PYROMETER_RANGE_BLOCKS = 24f;

    /**
     * Invented: the carried pressure gauge reads in half-Pu steps. The danger line is 5 Pu,
     * critical 7 and a boiler starts to seep at 3.5, so a half step lets the player see the
     * danger line coming without ever reading the exact figure.
     */
    public static final Pu PRESSURE_GAUGE_RESOLUTION_PU = new Pu(0.5f);

    /** The XP meter's step for a level: whole levels, so a finer reading needs a fitted circuit. [OPEN] until played */
    public static final float XP_METER_RESOLUTION_LEVEL = 1f;

    /** The XP meter's step for points held: fives. [OPEN] until played */
    public static final float XP_METER_RESOLUTION_POINTS = 5f;

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

    /** Calipers read a part's dimensions to this, in mm. */
    public static final float CALIPERS_RESOLUTION_MM = 0.1f;

    /** The survey meter: reach in blocks, the step its ore count reads to, and the wait between uses. Placeholders. */
    public static final int SURVEY_RADIUS = 16, SURVEY_COOLDOWN_TICKS = 40;

    public static final float SURVEY_RESOLUTION = 8f;

    /** Invented. Oil reads as a block count would: this many per block that the nearest basin's circle reaches into the survey radius. */
    public static final float SURVEY_OIL_PER_BLOCK = 4f;

    /**
     * Placeholder. What each electrical instrument can resolve (philosophy 10: tiers buy
     * significant figures, never access). The electroscope is coarse on purpose -- it says a wire
     * is live and roughly how hard, not how much is flowing.
     */
    public static final float GALVANOMETER_RESOLUTION_AMPS = 0.25f;

    public static final float ELECTROSCOPE_RESOLUTION_VOLTS = 0.5f;

    /** Placeholder. Noise floor, in steps: even a sound probe reads a little off (philosophy 10). */
    public static final float MASS_PROBE_NOISE_STEPS = 0.4f;

    /** Placeholder. Extra noise, in steps, for a probe cracked to its floor. */
    public static final float MASS_PROBE_CRACKED_NOISE_STEPS = 6f;

    /** Hydrometer: state of charge it reads to, as a share. */
    public static final float HYDROMETER_RESOLUTION = 0.05f;


    // --- the jar baton (wiki/Jar_Baton.md) -------------------------------------------------------

    /** Placeholder. Volts a baton's jar is rated for; charge above this, or a wet holder, shocks the holder. */
    public static final float BATON_RATING_VOLTS = ElectricTuning.ELECTRIC_HV_VOLTS;

    /** Placeholder. Stagger ticks per volt discharged, capped at {@link #BATON_STAGGER_MAX_TICKS}. */
    public static final float BATON_STAGGER_TICKS_PER_VOLT = 2f;

    public static final int BATON_STAGGER_MAX_TICKS = 60;

    /** Placeholder. Fraction of speed a discharged-into target loses while staggered. */
    public static final double BATON_STAGGER_SLOW = 0.5;

    /** Placeholder. Hand iron: slowest cooling rate (Tu/t) of the iron, heat each held tick draws into the joint, hottest a fire may leave it. */
    public static final TuRate IRON_COOLING_TU_PER_TICK = new TuRate(0.3f);

    public static final float IRON_MAX_TU = 450f;

    public static final int SOLDER_PAD_MAX_DWELL_TICKS = 200;

    public static final int SOLDER_LEAVE_LINGER_TICKS = 40;

    /** Placeholder. Component tester: finest step it tells apart on a 0-1 figure (response, condition). */
    public static final float TESTER_RESOLUTION = 0.05f;


    // --- basic electronics phase 4: every figure below is a placeholder ------------

    /** Placeholder. Circuit: parts one board carries; a piezo plate rings as a clock only at or above this coupling. */
    public static final int CIRCUIT_MAX_PARTS = 4;

    /** Placeholder. The step a dosimeter prints dose to (Ru x s). */
    public static final float DOSIMETER_RESOLUTION = 10f;

    /** Placeholder. A Geiger counter: clicks per second with nothing near, and per Ru; the resolution it prints to. */
    public static final float GEIGER_BACKGROUND_CPS = 0.4f, GEIGER_CPS_PER_RU = 2f, GEIGER_RESOLUTION = 0.1f;

    /** Placeholder. Charge one weld of the hand arc welder draws from a battery, in amp-ticks (about 50 A for the 40 ticks of the hold). */
    public static final float WELDER_CHARGE_PER_WELD = 2000f;

    /** Placeholder. Ticks the player holds the welder on a modular item in the other hand for one weld. */
    public static final int WELDER_USE_TICKS = 40;

    /** Placeholder. How far each weld lowers a part's maximum condition: a repaired part is never as good as new. */
    public static final float WELDER_MAX_STEP = 0.1f;

    /** Placeholder. The lowest maximum condition repeated welding can leave a part at. */
    public static final float WELDER_MAX_FLOOR = 0.5f;

    /** Placeholder. Condition the welder's own electrode loses per weld (it keeps working at {@link #WELDER_ELECTRODE_FLOOR}). */
    public static final float WELDER_ELECTRODE_WEAR = 0.01f;

    /** Placeholder. The worst a welder's electrode gets: it welds on, only worn. */
    public static final float WELDER_ELECTRODE_FLOOR = 0.25f;

    /** Placeholder. Filler metal for a part whose material cannot be told (a machine with no housing material): plain iron. */
    public static final net.minecraft.resources.ResourceLocation WELDER_DEFAULT_FILLER = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("feedback", "iron");

    /** The flux meter's step, as a fraction of full power. [OPEN] until played */
    public static final float FLUX_METER_RESOLUTION = 0.05f;
}
