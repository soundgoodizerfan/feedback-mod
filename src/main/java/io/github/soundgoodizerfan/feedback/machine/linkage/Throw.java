// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.linkage;

import net.minecraft.util.StringRepresentable;

/**
 * How far the crank throws, which is the only setting it has.
 *
 * <h2>Two options, deliberately</h2>
 * Philosophy 3: a crank throw that is short or long is a decision, while one that is any number
 * from 1 to 64 is a spreadsheet with a correct answer hidden in it.
 *
 * <h2>What a throw actually trades</h2>
 * A lever exchanges force for distance and the work per stroke is the same either way. So throw
 * does <em>not</em> change how fast a hammer flattens anything -- it changes only how hard each
 * blow lands, which matters when a material has a minimum force below which nothing happens.
 * <p>
 * <h2>Why this is the slice's sharpest lesson</h2>
 * Copper is soft enough that a short throw <em>overshoots a whole stage</em>: one blow leaves an
 * ingot, the second carries it past plate and straight into foil. Plate is not merely quick to
 * pass, it is unreachable at that setting. The long throw reaches plate in five and gives three
 * blows to grab it.
 * <p>
 * So the two settings do not make the same product faster or slower -- they make
 * <em>different products</em>, chosen by how the machine is geared rather than by any recipe
 * selector. A short-throw hammer is a foil machine.
 * <p>
 * It is also why the same part reads backwards on a bellows, where the useful output is
 * displaced air rather than work, and a longer stroke simply moves more of it.
 */
public enum Throw implements StringRepresentable {

    SHORT("short"),
    LONG("long");

    private final String name;

    Throw(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
