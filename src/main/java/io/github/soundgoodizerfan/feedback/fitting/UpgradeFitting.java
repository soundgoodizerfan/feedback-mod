// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
