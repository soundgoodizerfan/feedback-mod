// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular;

import java.util.Locale;

/**
 * A school of science or engineering, the width of a thing a person could have majored in. A type names its school in its data
 * ({@code "school": "mechanical"}); to draw a blueprint of such a type at the Blueprint Table you must hold that school's treatise
 * ({@link Treatises}). Types with no school are free to draw. A school is not a tier: a school is a subject, and knowing one tells nothing
 * of the others. The enum is the list of subjects (each has an item); which types belong to which is the types' own data.
 */
public enum School {
    /** Guns, crossbows and the like. */
    WEAPONS,
    /** Engines, gearboxes and the machinery that turns fuel into work. */
    MECHANICAL,
    /** Propellers and what flies. */
    AEROSPACE,
    /** Powered armour and its modules. */
    ROBOTICS,
    /** Pressure vessels, exchangers, boilers and the plant that holds and moves a process (chemical engineering). */
    CHEMICAL,
    /** Rolls, dies, bits and the parts a machine shop works metal with (materials engineering). */
    MATERIALS;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The school called {@code id}, or null (none, or one this build does not know). */
    public static School of(String id) {
        for (School s : values())
            if (s.id().equals(id))
                return s;
        return null;
    }
}
