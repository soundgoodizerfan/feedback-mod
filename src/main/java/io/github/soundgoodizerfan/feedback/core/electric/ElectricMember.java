// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.electric;

import io.github.soundgoodizerfan.feedback.core.unit.Eu;
import org.jetbrains.annotations.Nullable;

import io.github.soundgoodizerfan.feedback.core.unit.VoltageBand;

/**
 * What the resolver needs to know about one thing on an electrical run, and nothing about blocks.
 *
 * <h2>Why this is an interface and not the block entity</h2>
 * {@link ElectricRun} is pure arithmetic over a graph. Keeping it blind to the world is what lets
 * the resolver be tested without placing a block, and keeps the physics checkable the way
 * {@code Heat.tick} is: the file that does the sums is not also the file that does the plumbing.
 * {@link ElectricNode} is the block-entity implementation.
 *
 * <h2>Three roles, and a node has exactly one</h2>
 * A wire conducts. A source offers a band and a current. A sink wants a current at a rated
 * band. Nothing is two of these at once, so there is no node that is secretly a battery and a
 * load -- storage is a block of its own and arrives with the capacitor.
 *
 * <h2>Bands, not volts</h2>
 * A band is a packet size in Eu and amps are packets per tick, so {@code Eu/t = packet x amps}. A
 * wire loses a fixed Eu slice of each packet ({@link #lossPerPacket}); the band is unchanged and the
 * sink receives a smaller packet, which is why the resolver hands down a float Eu next to the band.
 * The typed seam is {@link #offered} and {@link #rated}, for display and anything that crosses to
 * another system.
 */
public interface ElectricMember {

    enum Role { WIRE, SOURCE, SINK }

    Role role();

    /** SOURCE: the band it puts on the line right now; null when idle. */
    @Nullable
    default VoltageBand sourceBand() {
        return null;
    }

    /** SOURCE: amps it holds its voltage up to. Zero when idle. */
    default float sourceAmps() {
        return 0;
    }

    /**
     * SOURCE: a store (battery, jar). A reserve pools with a same-band generator only for the current generation cannot cover, and
     * takes charge from the generation's surplus ({@link #reserveWantedAmps}); alone on its band it supplies as any source does.
     */
    default boolean reserve() {
        return false;
    }

    /** SOURCE, reserve: amps of charge it would take from a surplus right now. Zero when full. */
    default float reserveWantedAmps() {
        return 0;
    }

    /**
     * SINK: amps it wants right now. Zero when switched off. A WIRE may also answer a transient
     * figure here (a fence taking a hit); the resolver counts it as load at that wire.
     */
    default float wantedAmps() {
        return 0;
    }

    /** SINK: the band it is built for; its rated packet is {@link ElectricRules#packetEu}. */
    default VoltageBand ratedBand() {
        return VoltageBand.LV;
    }

    /**
     * Whether this member joins its neighbours at all. An open breaker or a blown fuse does not,
     * so it sits alone in a run of its own and the line through it is two runs. A member whose
     * answer changes calls {@link ElectricPropagator#reconnect}.
     */
    default boolean conducting() {
        return true;
    }

    /**
     * Whether this member will join {@code other} when they touch. Both must agree. A relay does
     * not join the electromagnet that moves it: they touch magnetically, not electrically.
     */
    default boolean joins(ElectricMember other) {
        return true;
    }

    /**
     * WIRE: a bare conductor, which arcs across a one-block air gap to another bare one in a
     * straight line (6.4). Read by the propagator's neighbour walk; a member whose answer changes
     * queues a rebuild with {@link ElectricPropagator#requeue}.
     */
    default boolean bare() {
        return false;
    }

    /** WIRE: Eu lost from each packet crossing this block, whatever the amps. Rises as the conductor degrades. */
    default float lossPerPacket() {
        return 0;
    }

    /**
     * The resolver's answer, once per tick, for every member of the run (zeros included, so a run
     * that lost its source reads zero rather than keeping its last figure).
     *
     * @param band    the run's band; it arrives unchanged along a wire. LV when nothing supplies.
     * @param eu      WIRE: Eu in a packet at this block. SINK: Eu in the packet it receives, after cable
     *                loss. SOURCE: Eu in the packet it sends. Zero when nothing arrives.
     * @param amps    WIRE: packets per tick through this block. SINK: delivered. SOURCE: drawn; negative for a reserve being charged.
     * @param wanted  SINK: current it asked for. Delivered below this is a starved sink.
     * @param blocked SOURCE only: it was active but on a run with a higher band, so it supplies
     *                nothing (mixed-band runs).
     */
    void onResolved(VoltageBand band, float eu, float amps, float wanted, boolean blocked);

    /** The typed form of what a source offers: its band and whole amps. For display and seams. */
    default Eu offered() {
        VoltageBand band = sourceBand();
        return new Eu(band == null ? VoltageBand.LV : band, Math.round(sourceAmps()));
    }

    /** The typed form of what a sink is built for: its band and the whole amps it wants. */
    default Eu rated() {
        return new Eu(ratedBand(), Math.round(wantedAmps()));
    }
}
