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
package io.github.soundgoodizerfan.feedback.core.thermal;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.unit.Conductance;
import io.github.soundgoodizerfan.feedback.core.unit.Tu;
import io.github.soundgoodizerfan.feedback.core.unit.TuRate;

/**
 * How heat moves. Every thermal figure in the mod comes out of these four lines.
 *
 * <h2>Why flow is driven by a difference</h2>
 * The first version had a fire deliver a flat {@code Work/t}, and it was wrong in a way worth
 * recording, because it is the obvious thing to write and it looks fine until insulation exists.
 * <p>
 * With a flat supply, a vessel settles where supply equals leak -- {@code T = fire / leak} -- so
 * halving the leak <em>doubles</em> the final temperature. Insulating a crucible made it run
 * away, and the slice's "large insulated crucible wins" became "large insulated crucible burns
 * the batch". The rule and the physics disagreed, and the physics was the thing that was made up.
 * <p>
 * Driving the flow on {@code (fire - vessel)} is the real relationship, and it fixes it by
 * construction: a vessel approaches its fire's temperature and can never pass it. Insulation then
 * means what it should -- you get closer to the flame and you hold there -- and the only way past
 * the flame is a hotter flame, which is what the bellows sells.
 * <p>
 * It also paid for something nobody was designing. Lava is a fire fixed at 1200 Tu, so a crucible
 * over lava sits just under 1200 Tu indefinitely: the most stable heat source in the game, and
 * permanently too cool for steel. The slice asked for exactly that and no rule had to be written.
 */
public final class Heat {

    private Heat() {
    }

    /**
     * Advance a body one tick, given the temperature of whatever is firing it.
     *
     * @param fireTu    the flame's temperature, or ambient when nothing is lit.
     * @param conducted whether the fire is actually in contact. A cold firebox is not a fire; it
     *                  is just more room to lose heat to.
     * @return how fast this body moved, signed. Callers watch this because a process can care
     *         how fast it was heated as well as how hot it got.
     */
    public static TuRate tick(ThermalBody body, Tu fireTu, boolean conducted) {
        // Unwrapped inline rather than into locals, deliberately. See core/unit's package javadoc:
        // the types guard the plumbing, and holding a bare `leak` and a bare `mass` side by side
        // in this method is exactly the swap they exist to prevent.
        float temperature = body.getTemperature().value();

        float fromFire = conducted
                ? FTuning.FIRE_CONDUCTANCE.workPerTickPerTu() * (fireTu.value() - temperature)
                : 0;
        float toRoom = body.getLeak().workPerTickPerTu()
                * (temperature - FTuning.AMBIENT_TU.value());

        float delta = (fromFire - toRoom) / Math.max(1f, body.getThermalMass().workPerTu());
        body.setTemperature(new Tu(temperature + delta));
        return new TuRate(delta);
    }

    /**
     * Move heat between two bodies with no fire and no ambient involved -- a
     * {@code HeatExchangerBlockEntity} standing between them. Same {@code conductance x
     * difference} relationship {@link #tick} drives a vessel with, just with the fire's infinite
     * reservoir replaced by a second finite body: flow still runs hot to cold and stops at
     * equality, and each side moves by {@code Work / its own thermal mass}, so a small body swings
     * further than a large one for the same Work crossing the link -- exactly {@link #tick}'s
     * mass term, applied twice.
     *
     * @param link how readily Work crosses the link -- a property of the exchanger, not of
     *             either body, the same way {@link io.github.soundgoodizerfan.feedback.core.unit.Conductance}
     *             is already a property of the boundary rather than of what is on either side of it.
     */
    public static void exchange(ThermalBody a, ThermalBody b, Conductance link) {
        float ta = a.getTemperature().value();
        float tb = b.getTemperature().value();
        float work = link.workPerTickPerTu() * (ta - tb);
        a.setTemperature(new Tu(ta - work / Math.max(1f, a.getThermalMass().workPerTu())));
        b.setTemperature(new Tu(tb + work / Math.max(1f, b.getThermalMass().workPerTu())));
    }

    /**
     * Where a body will end up, given a fire held at this temperature. Not used by the simulation
     * -- it is here because it is the figure every tuning argument is actually about, and working
     * it out by hand from {@link #tick} each time is how the numbers drifted the first time.
     */
    public static Tu equilibrium(ThermalBody body, Tu fireTu) {
        // The two conductances are added, which is only a legal thing to write because they share
        // a dimension. Nothing said so until Conductance existed; now the types do.
        float conductance = FTuning.FIRE_CONDUCTANCE.workPerTickPerTu();
        float leak = body.getLeak().workPerTickPerTu();
        return new Tu((conductance * fireTu.value() + leak * FTuning.AMBIENT_TU.value())
                / (conductance + leak));
    }

    /**
     * Where a body has fallen to after a span of ticks with nothing heating it.
     *
     * <h3>Linear, not exponential, and the reason is game design rather than physics</h3>
     * This was Newton's law first -- {@code ambient + (T - ambient) * exp(-t / tau)} -- which is
     * the correct relationship and is what {@link #tick} still uses for a vessel sitting on a
     * fire. It was wrong for a loose workpiece, and reading TerraFirmaCraft's treatment is what
     * made the reason obvious.
     * <p>
     * An exponential never arrives. It needed a "close enough to ambient" floor to stop every
     * ingot the player ever heated from carrying components forever, and that floor is a fudge
     * standing where a real figure should be. Worse, it puts almost all of the interesting time
     * in the first few seconds and then a very long, very flat tail in which nothing happens and
     * the player is still nominally holding a hot ingot.
     * <p>
     * Straight-line cooling arrives, at a tick the player can count. "You have about fifteen
     * seconds to get this to the anvil" becomes a true sentence rather than an approximation, so
     * the walk from the fire is a budget that can be planned against -- which is the entire point
     * of making the workpiece carry its heat. The rate being independent of temperature is
     * physically wrong and creates no decision either way, and the standing test is whether
     * modelling a variable creates an engineering choice. This one does not.
     *
     * @param rate Tu shed per tick.
     */
    public static Tu cooled(Tu fromTu, float ticks, TuRate rate) {
        if (ticks <= 0)
            return fromTu;
        return new Tu(Math.max(FTuning.AMBIENT_TU.value(),
                fromTu.value() - ticks * rate.tuPerTick()));
    }

    /**
     * How long a workpiece at this temperature stays above a floor, in ticks.
     * <p>
     * Only meaningful because the cooling is linear. It exists so the figure the player is
     * implicitly budgeting against can be stated exactly somewhere -- by a good enough instrument,
     * eventually, and by the tuning argument today.
     */
    public static float ticksAbove(Tu fromTu, Tu floorTu, TuRate rate) {
        if (fromTu.value() <= floorTu.value() || rate.tuPerTick() <= 0)
            return 0;
        return (fromTu.value() - floorTu.value()) / rate.tuPerTick();
    }
}
