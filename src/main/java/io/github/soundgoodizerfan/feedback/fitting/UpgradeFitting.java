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

import io.github.soundgoodizerfan.feedback.core.unit.ThermalMass;

/**
 * A fitting that doesn't occupy a face -- philosophy §4's upgrade, made attachable rather than
 * baked into a block's stats. A larger vessel is a different block; a flywheel or a finer
 * screen is one of these.
 * <p>
 * One figure so far, {@link #addedThermalMass()}, because one concrete upgrade
 * ({@code fitting.upgrade.RefractoryLiningFitting}) exists and it changes exactly that. A holder
 * reads it without asking what kind of upgrade it is -- the figure is the property, not the
 * class. Conductance, drag and the rest get a method each when an upgrade needs one; a second
 * implementer is the test of whether this shape is right, so it stays this small.
 */
public interface UpgradeFitting extends Fitting {

    /** What this upgrade adds to its holder's thermal mass; nothing, unless it says otherwise. */
    default ThermalMass addedThermalMass() {
        return new ThermalMass(0f);
    }
}
