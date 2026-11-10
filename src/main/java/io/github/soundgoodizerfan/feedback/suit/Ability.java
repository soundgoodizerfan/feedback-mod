// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.suit;

/**
 * What a powered suit can do, one wheel segment each. Each lives on the piece that carries its fitting
 * ({@link ExoSuit#available}): strength and plating on the chestplate, jump and dash on the legs and boots, night sight and thermal in the
 * helmet's sensor head.
 */
public enum Ability {
    STRENGTH, JUMP_DASH, PLATING, NIGHT, THERMAL;

    public int bit() {
        return 1 << ordinal();
    }

    public static Ability of(int ordinal) {
        Ability[] all = values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : null;
    }

    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
