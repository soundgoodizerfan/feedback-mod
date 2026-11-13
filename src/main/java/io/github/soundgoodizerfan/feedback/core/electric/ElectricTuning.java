// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.electric;



/**
 * Tuning constants for the core/electric code.
 * Every figure is a placeholder or invented unless its comment says it comes from the slice design.
 */
public final class ElectricTuning {

    private ElectricTuning() {
    }

    // --- electricity (electricity) ----------------------------------------

    //
    // Every figure here is a PLACEHOLDER. The band anchors are [OPEN] (6.3): the
    // reference volts below only give the four named bands an order of magnitude to be compared
    // against, a factor of four apart, and carry no claim about what a band is physically.
    // Voltages are plain volts in floats: a band is a rating (VoltageBand) and what a sink sees
    // is a continuous figure after path drop, which no closed list can name.
    /** Placeholder. Reference volts of each {@link io.github.soundgoodizerfan.feedback.core.unit.VoltageBand}. */
    public static final float ELECTRIC_LV_VOLTS = 1.5f, ELECTRIC_MV_VOLTS = 6f,
            ELECTRIC_HV_VOLTS = 24f, ELECTRIC_UHV_VOLTS = 96f, ELECTRIC_EHV_VOLTS = 384f, ELECTRIC_TRUNK_VOLTS = 1536f;

    /** Placeholder. Share of a band's reference volts a potential must reach to be in that band, so a source sagging a little stays in its band. */
    public static final float BAND_REACH = 0.9f;

    /**
     * Placeholder. Air gap, in blocks, a bare conductor arcs across to another bare
     * conductor in a straight line (6.4). Structural, not a dial: the neighbour walk asks about
     * exactly one air block, so this stays 1 unless that walk is widened with it.
     */
    public static final int ARC_GAP_BLOCKS = 1;

    /**
     * Placeholder, and [OPEN] (6.2): how far past its rated current a source can be drawn, as a
     * multiple of that rating, before it supplies no more. Below the rating the source holds its
     * voltage; between the rating and this it sags (see {@link #SOURCE_SAG_FLOOR}). Never destroyed.
     */
    public static final float SOURCE_OVERDRAW_LIMIT = 2f;

    /** Placeholder, and [OPEN] (6.2): the fraction of its voltage a source holds at {@link #SOURCE_OVERDRAW_LIMIT}. */
    public static final float SOURCE_SAG_FLOOR = 0.25f;

    /**
     * Invented, grid spec 4.3. Droop: a source within this fraction of the run's top voltage joins the pool and shares by rating;
     * the pool holds the rating-weighted mean of their volts. Further below is outbid, as before.
     */
    public static final float DROOP_TOLERANCE = 0.05f;

    /**
     * Placeholder (basic electronics phase 2). Sparks: a break of at least this
     * many amps makes one; it carries this far (a game event's radius is fixed per type).
     */
    public static final float SPARK_MIN_AMPS = 1f;

    public static final int SPARK_RANGE_BLOCKS = 16;
}
