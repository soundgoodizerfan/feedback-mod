// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import java.util.Locale;

/** What a bay holds and how. {@code PALLET} is the bare bay and has no item. */
public enum Fitting {
    PALLET, HOPPER, BIN;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Fitting byOrdinal(int i) {
        return i >= 0 && i < values().length ? values()[i] : PALLET;
    }
}
