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
    // A band is a packet size in Eu (GregTech's model): amps are packets per tick, Eu/t = packet x amps.
    // Geometric by band, x4 from GregTech's LV 32. The band anchors are [OPEN] (6.3); every figure is a placeholder.
    /** Placeholder. Eu in one packet of each {@link io.github.soundgoodizerfan.feedback.core.unit.VoltageBand}. */
    public static final float PACKET_LV_EU = 32f, PACKET_MV_EU = 128f, PACKET_HV_EU = 512f,
            PACKET_UHV_EU = 2048f, PACKET_EHV_EU = 8192f, PACKET_TRUNK_EU = 32768f;

    /**
     * Placeholder. Eu a shaft turning at 1 RPM under 1 Su carries into a generator (and a motor takes back out): the one exchange
     * between rotation load and electricity. A generator drawn {@code E} Eu/t loads its shaft {@code E / (this x RPM)} Su.
     */
    public static final float EU_PER_SU_RPM = 20f;

    /**
     * Placeholder. Air gap, in blocks, a bare conductor arcs across to another bare
     * conductor in a straight line (6.4). Structural, not a dial: the neighbour walk asks about
     * exactly one air block, so this stays 1 unless that walk is widened with it.
     */
    public static final int ARC_GAP_BLOCKS = 1;

    /**
     * Placeholder, and [OPEN] (6.2): how far past its rated current a source can be drawn, as a
     * multiple of that rating, before it supplies no more. Never destroyed.
     */
    public static final float SOURCE_OVERDRAW_LIMIT = 2f;

    /**
     * Placeholder (basic electronics phase 2). Sparks: a break of at least this
     * many amps makes one; it carries this far (a game event's radius is fixed per type).
     */
    public static final float SPARK_MIN_AMPS = 1f;

    public static final int SPARK_RANGE_BLOCKS = 16;
}
