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

    /**
     * Whether a store (battery, jar, charger) takes charge from a line: a packet arrives and its band is at or above the store's
     * rating. A band under the rating never charges it; a band over it charges it at the cost of {@link #overVoltage}.
     */
    public static boolean accepts(VoltageBand fed, float packetEu, VoltageBand rated) {
        return packetEu > 0f && fed.compareTo(rated) >= 0;
    }

    /**
     * The over-rating of a band met by a carrier that is not on a run (a battery in a hand, a gun's cell, a wearer's pack): the same
     * scale as {@link #overVoltage}, each band over a factor of four.
     */
    public static float overBand(VoltageBand fed, VoltageBand rated) {
        return Math.max(0f, packetEu(fed) / packetEu(rated) - 1f);
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
