// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.logistics.store;

import io.github.soundgoodizerfan.feedback.logistics.sort.Band;

/**
 * The bands recorded for a lot at intake, as a set of {@link Band} bits. A property no head covered has no bit; counts and
 * identity are always right, these are only what the dock's heads made of the goods.
 */
public final class Marks {

    private Marks() {
    }

    public static int bit(Band band) {
        return 1 << band.ordinal();
    }

    public static boolean has(int marks, Band band) {
        return (marks & bit(band)) != 0;
    }

    /** Whether {@code marks} records any band of {@code property}. */
    public static boolean hasProperty(int marks, io.github.soundgoodizerfan.feedback.logistics.sort.SortProperty property) {
        for (Band b : Band.values())
            if (b.property() == property && has(marks, b))
                return true;
        return false;
    }

    /** Every band of the property {@code band} belongs to. */
    private static int property(Band band) {
        int all = 0;
        for (Band b : Band.values())
            if (b.property() == band.property())
                all |= bit(b);
        return all;
    }

    /** {@code held} with each property {@code read} covers replaced by the newer reading. */
    public static int merge(int held, int read) {
        for (Band b : Band.values())
            if (has(read, b))
                held &= ~property(b);
        return held | read;
    }

    /**
     * Whether goods with {@code marks} pass the picked bands: for every property with any pick, one of that property's
     * picked bands is among the marks.
     */
    public static boolean passes(int marks, int picked) {
        for (Band b : Band.values()) {
            if (!has(picked, b))
                continue;
            if ((marks & picked & property(b)) == 0)
                return false;
        }
        return true;
    }
}
