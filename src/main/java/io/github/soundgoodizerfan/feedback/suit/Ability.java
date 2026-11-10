// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.suit;

/**
 * What a powered suit can do, one wheel segment each. The first five live on the piece that carries their fitting
 * ({@link ExoSuit#available}): strength and plating on the chestplate, jump and dash on the legs and boots, night sight and thermal in the
 * helmet's sensor head. The rest are given by an upgrade module held in an armour piece ({@link SuitModules#ability}): a segment is
 * available when a module that provides it is fitted. A {@link #pulse} fires once when chosen; a {@link #held} one is worked by its key.
 */
public enum Ability {
    STRENGTH, JUMP_DASH, PLATING, NIGHT, THERMAL, CLOAK, JET, SHOCK, SCAN, EMP,
    // the segments of modules that are otherwise passive: switches (the module works only while on) and pulses
    MAGNET, STRIDE, STEP, RANGE, LENS, HEAL, VENT, BURST, THORNS;

    public int bit() {
        return 1 << ordinal();
    }

    /** Chosen on the wheel it fires once and the switch does not stay on: the shockwave, scan ping, EMP, heal, vent, burst and thorns. */
    public boolean pulse() {
        return this == SHOCK || this == SCAN || this == EMP || this == HEAL || this == VENT || this == BURST || this == THORNS;
    }

    /** The first five: the ones the armour's own fittings give, always drawn on the wheel. */
    public boolean builtIn() {
        return ordinal() < 5;
    }

    public static Ability of(int ordinal) {
        Ability[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
    }

    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
