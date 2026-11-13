// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.treatise;

import java.util.Locale;

/**
 * A school of science or engineering, the width of a thing a person could have majored in. A type names its school in its data
 * ({@code "school": "mechanical"}, or {@code "mechanical:2"} for the second volume); to draw a blueprint of such a type at the Blueprint
 * Table you must hold that volume of the school's treatise ({@link Treatises}). Types with no school are free to draw. A school is not a
 * tier: a school is a subject, and knowing one tells nothing of the others. Within a school the treatise is numbered volumes ("Weapons
 * Treatise II: Firearms"), each its own subject: holding II does not give I. The enum is the list of subjects and how many volumes each
 * has (every volume has an item); which types belong to which is the types' own data.
 */
public enum School {
    /** Guns, crossbows and the like. */
    WEAPONS(3),
    /** Engines, gearboxes and the machinery that turns fuel into work. */
    MECHANICAL(2),
    /** Propellers and what flies. */
    AEROSPACE(2),
    /** Powered armour and its modules. */
    ROBOTICS(1),
    /** Pressure vessels, exchangers, boilers and the plant that holds and moves a process (chemical engineering). */
    CHEMICAL(3),
    /** Rolls, dies, bits and the parts a machine shop works metal with (materials engineering). */
    MATERIALS(1);

    private final int volumes;

    School(int volumes) {
        this.volumes = volumes;
    }

    /** How many volumes the school's treatise has. */
    public int volumes() {
        return volumes;
    }

    /** The bit of this school's volume {@code volume} (from 1) in a {@link Treatises#mask}: schools laid end to end. */
    public int bit(int volume) {
        int base = 0;
        for (School s : values()) {
            if (s == this)
                break;
            base += s.volumes;
        }
        return base + Math.max(1, Math.min(volume, volumes)) - 1;
    }

    /** The lang key of volume {@code volume}'s item. */
    public String itemKey(int volume) {
        return "item.feedback.treatise_" + id() + "_" + volume;
    }

    /** The registry path of volume {@code volume}'s item. */
    public String itemId(int volume) {
        return "treatise_" + id() + "_" + volume;
    }

    /** The volume a type's school field names ({@code "weapons:2"} is 2; no number is 1). */
    public static int volumeOf(String field) {
        int colon = field == null ? -1 : field.indexOf(':');
        if (colon < 0)
            return 1;
        try {
            return Math.max(1, Integer.parseInt(field.substring(colon + 1)));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The school a type's school field names ({@code "weapons"} or {@code "weapons:2"}), or null (none, or one this build does not know). */
    public static School of(String id) {
        if (id != null && id.indexOf(':') >= 0)
            id = id.substring(0, id.indexOf(':'));
        for (School s : values())
            if (s.id().equals(id))
                return s;
        return null;
    }
}
