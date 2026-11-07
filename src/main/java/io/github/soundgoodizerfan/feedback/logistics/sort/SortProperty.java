// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.sort;

import java.util.Locale;

/** A physical thing a sorter can read off cargo. Never an item id. */
public enum SortProperty {
    MAGNETIC, REFLECTIVITY, DENSITY, HARDNESS, FRICTION, KIND, TEMPERATURE, SIZE;

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
