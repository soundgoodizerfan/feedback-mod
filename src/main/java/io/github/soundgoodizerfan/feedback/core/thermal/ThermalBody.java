// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.thermal;

import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;

/**
 * Something that holds a temperature.
 *
 * <h2>Tu is a state, not an amount</h2>
 * The units table is strict about this and it shapes the whole interface. A body does not have
 * "some heat in it" that could be added to a bucket and carried elsewhere; it is <em>at</em> a
 * temperature, and what moves between bodies is {@code Work}. So there is no {@code getHeat()}
 * here and never will be. There is a temperature, a mass that says what a unit of Work does to
 * it, and a leak that says how fast the room takes it back.
 *
 * <h2>Why this is one interface across vessels and workpieces</h2>
 * Philosophy 9: a hot item is hot wherever it is, which means the thing that knows how to cool
 * is not the crucible. It is the general relationship, and the crucible, the firebox and the
 * ingot in your pocket are all cases of it. Writing it once is also the only way the numbers
 * stay consistent -- two implementations of Newton's law drift apart the first time one of them
 * is tuned.
 */
public interface ThermalBody {

    /** Current temperature. */
    Tu getTemperature();

    /** What the item held in a slot of this body is at: a body that lags its contents says so; the default is the body's own temperature. */
    default Tu slotTemperature(int slot) {
        return getTemperature();
    }

    void setTemperature(Tu tu);

    /**
     * Work required to raise this body by one Tu.
     * <p>
     * This is the number an upgrade changes. A larger vessel is not faster or more efficient --
     * it is heavier, and everything the player notices about it follows from that.
     */
    ThermalMass getThermalMass();

    /**
     * How fast this body bleeds Work to the room, per Tu it sits above ambient.
     *
     * <h3>A leak is a conductance, and the type now says so</h3>
     * This returns the same type as a fire's grip on the vessel above it, because they are the
     * same quantity -- {@code Work/t} per Tu of difference across a boundary. Calling one a leak
     * and the other a conductance is a statement about whether you wanted it, not about what it
     * is, and {@link Heat#equilibrium} has been adding the two together all along.
     */
    default Conductance getLeak() {
        return ThermalTuning.VESSEL_LEAK;
    }

    /**
     * Whether this body can be watched <em>passively</em> -- an ambient carried reading, a Jade
     * card, and eventually a cover's auto-attached sensor. It says nothing about a one-off manual
     * reading; see the next paragraph.
     *
     * <h3>What this wall actually gates, after getting it wrong once</h3>
     * The first version of this rule made a sealed vessel refuse <em>any</em> instrument outright,
     * on the theory that "sealed" and "measurable" should be the same fact asked two ways. That
     * conflated two different actions that cost two different things: watching a gauge is free and
     * continuous, sticking a thermometer through the door for one reading is a real, costed action
     * -- the same distinction {@code CalipersItem} already draws between reading an item you are
     * holding and reading a machine you have to walk up to and touch (§8, "a reading is passive
     * when taking it is free, and an action when taking it costs something"). A sealed vessel can
     * still be dipped into by hand; what it refuses is a sensor that watches it for nothing, which
     * is exactly what a cover or a HUD card would be. See {@code ThermometerItem#onItemUseFirst}
     * for the manual path this no longer blocks.
     * <p>
     * The wall this still builds is a real one, just narrower than originally stated: the
     * player's best sealed vessel cannot be <em>automated</em> against, because a controller needs
     * a continuous signal and a manual dip only ever answers for the instant it was taken. The way
     * out for automation is still a vessel designed to be watched, which is what the crucible is
     * for -- it was never really competing with a spot check.
     */
    default boolean hasThermowell() {
        return true;
    }
}
