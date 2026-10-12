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
package io.github.soundgoodizerfan.feedback.machine.linkage;
import io.github.soundgoodizerfan.feedback.core.unit.Su;

/**
 * Implemented by a machine that is driven by strokes rather than by rotation.
 * <p>
 * The split is the point. A hammer goes up and down and a shaft goes round and round, so
 * something has to convert one into the other, and that something is a component the player
 * places rather than a detail hidden inside the machine.
 */
public interface Reciprocating {

    /**
     * One stroke has been delivered.
     *
     * <h3>The linkage delivers a stroke, not a quantity</h3>
     * What arrives is the shape of the stroke -- which throw is installed and how much the gearing
     * multiplies it -- and the machine turns that into its own unit: force ({@code St}) on a
     * hammer, displaced air ({@code mB}) on a bellows. The linkage used to do the multiplying and
     * hand over {@code St}, which made the bellows pass a volume of air through a force unit
     * one-to-one. Nothing about a crank knows which of the two it is driving, so neither should
     * its arithmetic.
     *
     * <h3>The figure is raw, and the machine clamps it</h3>
     * Throw times gear advantage may be more than the machine's own {@code getMaxStrength()}. The
     * linkage deliberately does not clamp it, because clamping in the linkage threw the surplus
     * away with nothing able to notice: over-gearing became a silent no-op and the ceiling was
     * undiscoverable.
     * <p>
     * So the ceiling stays a declaration and enforcing it is the machine's own business, which is
     * also the only place that knows what the excess <em>does</em>. A hammer takes it as damage; a
     * bellows takes it as nothing at all, because there is no more air inside it to find. Same
     * stroke, two honest answers.
     *
     * @param installed     the throw on the crank.
     * @param gearAdvantage how much the drive's gearing multiplies the throw's figure -- a bare
     *                      ratio, the reciprocal of the speed ratio.
     */
    void onStroke(Throw installed, float gearAdvantage);

    /** Su this machine draws while being driven. */
    Su getLoadSu();
}
