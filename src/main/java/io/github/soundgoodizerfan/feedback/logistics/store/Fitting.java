// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import java.util.Locale;

/**
 * What a bay holds and how, and what is done to it. A bay takes one container fitting and one optional treatment
 * ({@link #treatment}); {@code PALLET} is the bare bay (or no treatment) and has no item.
 */
public enum Fitting {
    PALLET, HOPPER, BIN, CABINET, COLD_PLATE, LINER;

    /** A cold plate or a sealed liner: it goes on a bay beside its container, not instead of it. */
    public boolean treatment() {
        return this == COLD_PLATE || this == LINER;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Fitting byOrdinal(int i) {
        return i >= 0 && i < values().length ? values()[i] : PALLET;
    }
}
