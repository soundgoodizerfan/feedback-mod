// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.modular.treatise;

import java.util.Locale;

/**
 * A school of science or engineering, the width of a thing a person could have majored in. A type names its school in its data
 * ({@code "school": "mechanical"}, or {@code "mechanical:2"} for the second volume, or several joined by {@code +}: {@code "electrical:3+materials:2"}
 * wants both, and taking one apart teaches the first); to draw a blueprint of such a type at the Blueprint
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
    AEROSPACE(4),
    /** Powered armour and its modules, then whole robots. */
    ROBOTICS(2),
    /** Pressure vessels, exchangers, boilers and the plant that holds and moves a process (chemical engineering). */
    CHEMICAL(3),
    /** Machine heads, then smelting, annealing and the heat a metal is worked and treated by (materials engineering). */
    MATERIALS(2),
    /** Cells and static charge, coils, magnets and motors, then circuits: what makes, moves, holds and decides on charge (electrical engineering). */
    ELECTRICAL(4),
    /** Fission: the reactor, its fuel and what keeps it from running away (nuclear engineering). */
    NUCLEAR(1),
    /** Experience: carrying and holding it, drawing it from the world, then converting it to Work and spending it. */
    EXPERIENCE(3);

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
        if (field != null && field.indexOf('+') >= 0)
            field = field.substring(0, field.indexOf('+'));
        int colon = field == null ? -1 : field.indexOf(':');
        if (colon < 0)
            return 1;
        try {
            return Math.max(1, Integer.parseInt(field.substring(colon + 1)));
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /** One school and volume a field names. */
    public record Need(School school, int volume) {
    }

    /**
     * Every volume a field names. A field is one or more entries joined by {@code +} ({@code "electrical:2+materials:2"}): to meet it you
     * hold all of them. Entries naming no known school are left out. The first entry is the field's own volume (what taking it apart teaches).
     */
    public static java.util.List<Need> needs(String field) {
        java.util.List<Need> out = new java.util.ArrayList<>();
        if (field == null || field.isEmpty())
            return out;
        for (String entry : field.split("\\+")) {
            School s = of(entry.trim());
            if (s != null)
                out.add(new Need(s, Math.min(volumeOf(entry.trim()), s.volumes())));
        }
        return out;
    }

    /** The bits ({@link #bit}) of every volume a field names, or 0 for a field naming none. */
    public static int need(String field) {
        int mask = 0;
        for (Need n : needs(field))
            mask |= 1 << n.school.bit(n.volume);
        return mask;
    }

    /** Whether a held-volumes mask ({@link Treatises#mask}) meets a field: every volume it names is held. */
    public static boolean met(int mask, String field) {
        int need = need(field);
        return (mask & need) == need;
    }

    /** The first volume a field names that {@code mask} does not hold, or null when it is met. */
    public static Need missing(int mask, String field) {
        for (Need n : needs(field))
            if ((mask & (1 << n.school.bit(n.volume))) == 0)
                return n;
        return null;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The school a type's school field names ({@code "weapons"} or {@code "weapons:2"}), or null (none, or one this build does not know). */
    public static School of(String id) {
        if (id != null && id.indexOf('+') >= 0)
            id = id.substring(0, id.indexOf('+'));
        if (id != null && id.indexOf(':') >= 0)
            id = id.substring(0, id.indexOf(':'));
        for (School s : values())
            if (s.id().equals(id))
                return s;
        return null;
    }
}
