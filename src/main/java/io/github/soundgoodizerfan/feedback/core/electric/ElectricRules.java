// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.electric;

import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;

/**
 * The electrical rulings that are still open, each in one method so changing one is a one-line
 * edit and not a search.
 *
 * <p>Everything here is a decision the user has not closed, or a placeholder behind one. The
 * resolver ({@link ElectricRun}) and the sinks call these and hold no opinion of their own, which
 * is the whole reason they are not inlined where they are used.
 */
public final class ElectricRules {

    private ElectricRules() {
    }

    /** Eu in one packet of a band (GregTech's voltage tier): {@code Eu/t = packetEu x amps}. */
    public static float packetEu(VoltageBand band) {
        return switch (band) {
            case LV -> ElectricTuning.PACKET_LV_EU;
            case MV -> ElectricTuning.PACKET_MV_EU;
            case HV -> ElectricTuning.PACKET_HV_EU;
            case UHV -> ElectricTuning.PACKET_UHV_EU;
            case EHV -> ElectricTuning.PACKET_EHV_EU;
            case TRUNK -> ElectricTuning.PACKET_TRUNK_EU;
        };
    }

    /** Bookkeeping: the Eu per tick of {@code amps} packets of a band. */
    public static float euPerTick(VoltageBand band, float amps) {
        return packetEu(band) * amps;
    }

    /** Reference electrochemical potential of a band, for devices that compare a cell or electrode with what a line offers. Placeholder anchors, [OPEN] (6.3). */
    public static float bandPotential(VoltageBand band) {
        return switch (band) {
            case LV -> ElectricTuning.BAND_POTENTIAL_LV;
            case MV -> ElectricTuning.BAND_POTENTIAL_MV;
            case HV -> ElectricTuning.BAND_POTENTIAL_HV;
            case UHV -> ElectricTuning.BAND_POTENTIAL_UHV;
            case EHV -> ElectricTuning.BAND_POTENTIAL_EHV;
            case TRUNK -> ElectricTuning.BAND_POTENTIAL_TRUNK;
        };
    }

    /**
     * What an electrochemical device (battery, jar, charger) sees on a line, as a potential: the band's
     * reference, less in proportion to the packet that survived the cable loss. Never shown to a player.
     */
    public static float seenPotential(VoltageBand band, float packetEu) {
        return packetEu <= 0f ? 0f : bandPotential(band) * Math.min(1f, packetEu / packetEu(band));
    }

    /**
     * The band a potential belongs to: the highest whose reference it reaches within {@link ElectricTuning#BAND_REACH}, LV below all.
     * A cell at 1.1 V is LV; a 2 V battery cell is LV, a 12 V stack MV, 24 V HV (ruled 2026-11-03 for the battery).
     */
    public static VoltageBand bandOfPotential(float potential) {
        VoltageBand found = VoltageBand.LV;
        for (VoltageBand band : VoltageBand.values())
            if (potential >= bandPotential(band) * ElectricTuning.BAND_REACH)
                found = band;
        return found;
    }

    /**
     * The over-rating of a potential an electrochemical device or a held charge meets, against the reference of a rated band: the same
     * scale as {@link #overVoltage}, for a carrier of a charge that is not on a run (a battery in a hand, a gun's cell).
     */
    public static float overPotential(float potential, VoltageBand rated) {
        return Math.max(0f, potential / bandPotential(rated) - 1f);
    }

    /**
     * How fast a sink runs, 0 to 1: the product of the fraction of its rated packet it receives
     * (cable loss and a lower band both cut it, a higher band does not raise it) and the fraction
     * of its wanted current it gets. Zero floor, never a refusal (6.5). <b>[OPEN]</b> whether the
     * slow-down is in packet, in current or in their product.
     */
    public static float rate(float packetEu, float ampsDelivered, float ampsWanted, VoltageBand rated) {
        float eu = Math.min(1f, Math.max(0f, packetEu) / packetEu(rated));
        float amps = ampsWanted <= 0f ? 1f : Math.min(1f, ampsDelivered / ampsWanted);
        return eu * amps;
    }

    /**
     * Fraction by which the band a sink is fed is over its rating, 0 when it is not (or nothing arrives):
     * the packet ratio less one, so each band over is a factor of four. This is the force that cannot go into
     * the work (6.5): the caller turns it into condition loss and heat in its own body. <b>[OPEN]</b> the per-unit scale.
     */
    public static float overVoltage(VoltageBand fed, float packetEu, VoltageBand rated) {
        if (packetEu <= 0f)
            return 0f;
        return Math.max(0f, packetEu(fed) / packetEu(rated) - 1f);
    }
}
