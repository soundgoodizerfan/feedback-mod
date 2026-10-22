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
package io.github.soundgoodizerfan.feedback.core.electric;

import io.github.soundgoodizerfan.feedback.core.unit.Eu;
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
 * A wire conducts. A source offers a voltage and a current. A sink wants a current at a rated
 * band. Nothing is two of these at once, so there is no node that is secretly a battery and a
 * load -- storage is a block of its own and arrives with the capacitor (mechanics 6.6).
 *
 * <h2>Plain floats, not {@link Eu}</h2>
 * What a sink sees is a continuous figure after path drop, which no band can name and which an
 * {@code Eu} (a band and whole amps) cannot hold. The typed seam is {@link #offered} and
 * {@link #rated}, for display and for anything that crosses to another system; the resolver
 * unwraps inline, per the house rule that the physics reads like the equation.
 */
public interface ElectricMember {

    enum Role { WIRE, SOURCE, SINK }

    Role role();

    /** SOURCE: volts it puts on the line right now. Zero when idle. Nominal, before any sag. */
    default float sourceVolts() {
        return 0;
    }

    /** SOURCE: amps it holds its voltage up to. Zero when idle. */
    default float sourceAmps() {
        return 0;
    }

    /**
     * SINK: amps it wants right now. Zero when switched off. A WIRE may also answer a transient
     * figure here (a fence taking a hit); the resolver counts it as load at that wire.
     */
    default float wantedAmps() {
        return 0;
    }

    /** SINK: the band it is built for. Its rated volts are {@link ElectricRules#bandVolts}. */
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

    /** WIRE: volts lost across this block per amp through it. Rises as the conductor degrades. */
    default float dropPerAmp() {
        return 0;
    }

    /**
     * The resolver's answer, once per tick, for every member of the run (zeros included, so a run
     * that lost its source reads zero rather than keeping its last figure).
     *
     * @param volts   WIRE: the potential at this block. SINK: the voltage it sees. SOURCE: the
     *                voltage it is actually holding, after sag.
     * @param amps    WIRE: current through this block. SINK: current delivered. SOURCE: current drawn.
     * @param wanted  SINK: current it asked for. Delivered below this is a starved sink.
     * @param blocked SOURCE only: it was active but on a run with a higher voltage, so it supplies
     *                nothing (mechanics 6.2, mixed-voltage runs).
     */
    void onResolved(float volts, float amps, float wanted, boolean blocked);

    /** The typed form of what a source offers: its band and whole amps. For display and seams. */
    default Eu offered() {
        return new Eu(ElectricRules.bandOf(sourceVolts()), Math.round(sourceAmps()));
    }

    /** The typed form of what a sink is built for: its band and the whole amps it wants. */
    default Eu rated() {
        return new Eu(ratedBand(), Math.round(wantedAmps()));
    }
}
