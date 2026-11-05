/*
 * Feedback -- a Minecraft technology mod.
 * Copyright (C) 2026 soundgoodizerfan
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <https://www.gnu.org/licenses/>.
 *
 * Assets under src/main/resources/assets are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.logistics.sort;

import java.util.Locale;

/**
 * A named band of one property: the only way a sorter is set (a small choice, never a typed number).
 * A Java enum on purpose; the cuts live in {@code LogisticsTuning}.
 */
public enum Band {
    PULLED(SortProperty.MAGNETIC), IGNORED(SortProperty.MAGNETIC),
    DULL(SortProperty.REFLECTIVITY), MID_SHEEN(SortProperty.REFLECTIVITY), BRIGHT(SortProperty.REFLECTIVITY),
    LIGHT(SortProperty.DENSITY), MID_DENSITY(SortProperty.DENSITY), HEAVY(SortProperty.DENSITY),
    SOFT(SortProperty.HARDNESS), MID_HARD(SortProperty.HARDNESS), HARD(SortProperty.HARDNESS),
    SLICK(SortProperty.FRICTION), MID_GRIP(SortProperty.FRICTION), GRIPPY(SortProperty.FRICTION),
    METALLIC(SortProperty.KIND), STONY(SortProperty.KIND), GRANULAR(SortProperty.KIND), ORGANIC(SortProperty.KIND),
    POLYMER(SortProperty.KIND), OTHER(SortProperty.KIND),
    COLD(SortProperty.TEMPERATURE), WARM(SortProperty.TEMPERATURE), HOT(SortProperty.TEMPERATURE),
    SMALL(SortProperty.SIZE), MID_SIZE(SortProperty.SIZE), LARGE(SortProperty.SIZE);

    private final SortProperty property;

    Band(SortProperty property) {
        this.property = property;
    }

    public SortProperty property() {
        return property;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The next band in the same property (wrapping), for a module's cycle click. */
    public Band next() {
        Band[] all = values();
        for (int i = 1; i <= all.length; i++) {
            Band b = all[(ordinal() + i) % all.length];
            if (b.property == property)
                return b;
        }
        return this;
    }

    /** The first band of {@code p}. */
    public static Band first(SortProperty p) {
        for (Band b : values())
            if (b.property == p)
                return b;
        throw new IllegalStateException();
    }

    public static Band byOrdinal(int i) {
        Band[] all = values();
        return i >= 0 && i < all.length ? all[i] : PULLED;
    }
}
