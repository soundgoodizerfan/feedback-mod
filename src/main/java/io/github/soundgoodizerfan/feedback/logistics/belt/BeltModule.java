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
package io.github.soundgoodizerfan.feedback.logistics.belt;

import java.util.Locale;

import io.github.soundgoodizerfan.feedback.logistics.sort.Detector;
import io.github.soundgoodizerfan.feedback.logistics.sort.Gear;
import io.github.soundgoodizerfan.feedback.logistics.sort.SortProperty;

/**
 * A module occupying a belt block's own space. Each selects a fate for cargo reaching the block's
 * centre; none knows an item id. {@code powered} ones draw a little {@code Eu} and do nothing
 * without it; {@code sided} ones act on the side face they were placed against.
 */
public enum BeltModule {
    FUNNEL_IN(false, true, null, null, null),
    FUNNEL_OUT(false, true, null, null, null),
    CHUTE(false, true, null, null, null),
    DIVERTER(false, true, null, null, null),
    SPLITTER(false, false, null, null, null),
    MAGNET(false, true, Detector.MAGNET, Gear.CRUDE, SortProperty.MAGNETIC),
    ELECTROMAGNET(true, true, Detector.ELECTROMAGNET, Gear.OPTICAL, SortProperty.MAGNETIC),
    SCREEN(false, false, null, Gear.CRUDE, null),
    AIR_JET(false, true, Detector.JET, Gear.CRUDE, SortProperty.DENSITY),
    OPTICAL(true, true, Detector.OPTICAL, Gear.OPTICAL, SortProperty.REFLECTIVITY),
    WEIGHER(true, true, Detector.WEIGHER, Gear.OPTICAL, SortProperty.DENSITY),
    COUNTER(false, false, null, null, null),
    COOLER(false, false, null, null, null),
    GATE(false, false, null, null, null),
    FAN(true, false, null, null, null),
    /** A cover over the cell: rain and fluid above no longer wet the band. */
    HOOD(false, false, null, null, null),
    /** A coil replacing a roller: while powered it drives the run at a fixed speed, shaft or none. */
    LINEAR(true, false, null, null, null);

    public final boolean powered, sided;
    /** The head a sorting module reads with, or null. */
    public final Detector detector;
    /** What it does with cargo it cannot place, or null for a module that does not sort. */
    public final Gear gear;
    /** The property it reads (a weigher also cycles to size), or null. */
    public final SortProperty property;

    BeltModule(boolean powered, boolean sided, Detector detector, Gear gear, SortProperty property) {
        this.powered = powered;
        this.sided = sided;
        this.detector = detector;
        this.gear = gear;
        this.property = property;
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Whether a click on the placed module cycles its band (optical, weigher). */
    public boolean banded() {
        return this == OPTICAL || this == WEIGHER;
    }

    /** Whether it reads cargo and sweeps matches to its side. */
    public boolean sorts() {
        return detector != null;
    }

    /** Whether the head roller may carry it: only the end-of-run modules that need no band, screen or wire. */
    public boolean onHead() {
        return this == FUNNEL_OUT || this == CHUTE || this == GATE || this == DIVERTER || this == SPLITTER;
    }

    public static BeltModule byOrdinal(int i) {
        BeltModule[] all = values();
        return i >= 0 && i < all.length ? all[i] : null;
    }
}
