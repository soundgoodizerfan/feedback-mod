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
package io.github.soundgoodizerfan.feedback.process;

/**
 * How force is delivered to a workpiece -- the axis {@link Deformation} was missing as long as
 * every entry was a blow.
 *
 * <h2>Why this exists now and not from the start</h2>
 * {@code HandToolItem} used to say, correctly at the time, that there should be no registry of
 * which tools do which operations until an operation existed that was not deformation. Wire
 * drawing is still deformation -- same {@code work}/{@code hardness}/{@code result} shape, same
 * overshoot-cascades-forward rule -- but it is a different physical action on the same class of
 * material: a die pulls a rod thinner in tension, a hammer squashes a plate thinner in
 * compression. The same input item could plausibly answer to either with a different result, and
 * once that is possible {@link DeformationTable} needs a second key or two entries collide.
 *
 * <h2>Still not a registry</h2>
 * Each tool declares its own operation the same way it already declares its own {@code St} --
 * {@code HandToolItem#getOperation()}, one method per tool, no lookup table anywhere mapping
 * tool classes to behaviour.
 */
public enum Operation {
    /** Compressive impact -- a hammer, or a press's single large stroke. */
    BLOW,
    /** Tensile pull through a die -- a draw plate or a wire drawer. */
    DRAW,
    /**
     * Continuous compression between a driven roller pair -- a rolling mill.
     * <p>
     * Not {@code BLOW}: an ingot rolled into rod and an ingot hammered into a plate are the same
     * material meeting two different physical actions, and giving them one operation would mean
     * one of the two entries silently shadowing the other in {@link DeformationTable#find} the
     * moment both existed for the same input item -- the exact collision this enum was created to
     * avoid in the first place (see the class doc). Rolling produces an elongated, round-ish
     * cross-section a flat blow cannot; that is a real physical difference, not a rules technicality.
     */
    ROLL,
    /**
     * Material is subtracted rather than moved -- drilling, turning, grinding. Not looked up in
     * {@link DeformationTable} at all; {@code RemovalTable} matches on the input item alone,
     * since removal is one physical family with one gate (tool hardness over workpiece hardness),
     * not several actions competing for the same input the way {@code BLOW}/{@code DRAW}/{@code
     * ROLL} do. This value exists purely so {@code FDataComponents.WORK_OPERATION} can tell
     * removal progress apart from deformation progress on the same item -- see {@code
     * process/Removing.java}.
     */
    REMOVE
}
