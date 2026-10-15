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
     * @param ambient   the room this body sits in -- {@link AmbientTable#at}, which differs by
     *                  dimension.
     * @param conducted whether the fire is actually in contact. A cold firebox is not a fire; it
     *                  is just more room to lose heat to.
     * @return how fast this body moved, signed. Callers watch this because a process can care
     *         how fast it was heated as well as how hot it got.
     */
    public static TuRate tick(ThermalBody body, Tu fireTu, Tu ambient, boolean conducted) {
        // Leak, mass and conductance are unwrapped inline, deliberately -- see core/unit's package
        // javadoc: holding a bare `leak` and a bare `mass` side by side is exactly the swap the
        // types exist to prevent. The temperature is the one local: it is read three times, has
        // no same-typed neighbour to be swapped with, and must be read before it is written.
        float temperature = body.getTemperature().value();

        float fromFire = conducted
                ? FTuning.FIRE_CONDUCTANCE.workPerTickPerTu() * (fireTu.value() - temperature)
                : 0;
        float toRoom = body.getLeak().workPerTickPerTu()
                * (temperature - ambient.value());

        float delta = (fromFire - toRoom) / Math.max(1f, body.getThermalMass().workPerTu());
        body.setTemperature(new Tu(temperature + delta));
        return new TuRate(delta);
    }

    /**
     * {@link #tick} applied {@code ticks} times, in one step.
     *
     * <h3>Why this is exact and not an approximation</h3>
     * One tick moves a body by {@code -k (T - Teq)} where {@code k} is the total conductance over
     * the mass and {@code Teq} is {@link #equilibrium}. That is a geometric series, so after
     * {@code n} ticks the gap to equilibrium has shrunk by {@code (1 - k)^n} -- the same figure
     * {@link #tick} reaches by looping, in the same floats' worth of precision. It exists for
     * {@link Catchup}: a block back from an unloaded chunk was not ticked, and a million ticks
     * should cost what one does.
     * <p>
     * A {@code k} of one or more means the body reaches equilibrium inside a tick (or would
     * oscillate across it, which {@link #tick} does not model either); it is taken as arriving.
     */
    public static void relax(ThermalBody body, Tu fireTu, Tu ambient, boolean conducted, long ticks) {
        if (ticks <= 0)
            return;
        float fire = conducted ? FTuning.FIRE_CONDUCTANCE.workPerTickPerTu() : 0f;
        float leak = body.getLeak().workPerTickPerTu();
        float total = fire + leak;
        if (total <= 0f)
            return;
        float equilibrium = (fire * fireTu.value() + leak * ambient.value()) / total;
        float k = total / Math.max(1f, body.getThermalMass().workPerTu());
        float keep = k >= 1f ? 0f : (float) Math.pow(1f - k, ticks);
        float now = body.getTemperature().value();
        body.setTemperature(new Tu(equilibrium + (now - equilibrium) * keep));
    }

    /**
     * Put Work straight into a body with no fire involved -- an electrical conductor carrying
     * more than it is rated for. Just the mass term of {@link #tick}: what a unit of Work does
     * to a body is a property of the body, wherever the Work came from. The leak is still
     * {@link #tick}'s, so a body fed this way settles where input equals leak and never runs away.
     */
    public static void warm(ThermalBody body, float work) {
        body.setTemperature(new Tu(body.getTemperature().value()
                + work / Math.max(1f, body.getThermalMass().workPerTu())));
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
        // Inline, not a ta/tb pair of locals -- the swap the unit rule names. Each body is read
        // before it is written, and a's write does not touch b.
        float work = link.workPerTickPerTu() * (a.getTemperature().value() - b.getTemperature().value());
        a.setTemperature(new Tu(a.getTemperature().value() - work / Math.max(1f, a.getThermalMass().workPerTu())));
        b.setTemperature(new Tu(b.getTemperature().value() + work / Math.max(1f, b.getThermalMass().workPerTu())));
    }

    /**
     * Where a body will end up, given a fire held at this temperature. Not used by the simulation
     * -- it is here because it is the figure every tuning argument is actually about, and working
     * it out by hand from {@link #tick} each time is how the numbers drifted the first time.
     */
    public static Tu equilibrium(ThermalBody body, Tu fireTu, Tu ambient) {
        // The two conductances are added, which is only a legal thing to write because they share
        // a dimension. Nothing said so until Conductance existed; now the types do.
        return new Tu((FTuning.FIRE_CONDUCTANCE.workPerTickPerTu() * fireTu.value()
                + body.getLeak().workPerTickPerTu() * ambient.value())
                / (FTuning.FIRE_CONDUCTANCE.workPerTickPerTu() + body.getLeak().workPerTickPerTu()));
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
     * @param ambient where it stops -- the model is unchanged by a hotter room, only the floor moves.
     */
    public static Tu cooled(Tu fromTu, float ticks, TuRate rate, Tu ambient) {
        if (ticks <= 0)
            return fromTu;
        return new Tu(Math.max(ambient.value(),
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
