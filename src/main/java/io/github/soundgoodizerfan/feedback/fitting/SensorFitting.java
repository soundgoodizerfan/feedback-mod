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
package io.github.soundgoodizerfan.feedback.fitting;

import io.github.soundgoodizerfan.feedback.control.data.DataNode;
import io.github.soundgoodizerfan.feedback.instrument.Instrument;

import net.minecraft.network.chat.Component;

/**
 * An {@link Instrument} that watches instead of being carried -- the temperature sensor fitting,
 * the Lathe reading its own cut, and whatever eventually reads pressure or load the same way.
 * (The bimetallic strip is not one: it is its own block, wired straight to a switch, with no
 * reading for a controller to ask for.)
 *
 * <h2>Why this is an {@code Instrument} rather than its own reading contract</h2>
 * A sensor fitting and a carried instrument answer the exact same three questions -- what it
 * can read, how finely, and what to call it (§8's significant-figures point doesn't care who is
 * holding the device). What differs is only how each gets its chance to look: an
 * {@code Instrument} is found by {@code Instruments.best()} rummaging the player's inventory;
 * a {@code SensorFitting} is found already attached, by {@link Fittable#getSidedFitting}. Two
 * lookup paths to the one contract, not two contracts.
 * <p>
 * {@code ThermalBody.hasThermowell()} still gates whether a sensor fitting can be mounted at all
 * on a given holder, exactly as it already gates a carried thermometer's passive reading -- see
 * {@link Fittable#canMount} and that method's own doc. A holder that refuses the mount never
 * produces a fitting to read from in the first place.
 *
 * <h2>How a reading actually reaches a controller</h2>
 * A {@link DataNode}, pulled rather than pushed: nothing here schedules a tick or fires an event
 * when the reading changes. Whatever is linked to this fitting -- a controller -- asks {@link #readValue()} when it wants to know, the same on-demand shape
 * {@code ThermalBody} already uses for temperature. See {@code control/data} for the link graph.
 */
public non-sealed interface SensorFitting extends Instrument, SidedFitting, DataNode {

    /** The current reading, labelled and quantised at this fitting's own resolution. */
    Component readValue();

    /**
     * The same quantised figure as {@link #readValue()}, unlabelled -- what a controller's
     * {@code Input} card actually consumes. A program can never see a number the player
     * couldn't also read: both methods are built on the one quantised value.
     */
    float readRaw();
}
