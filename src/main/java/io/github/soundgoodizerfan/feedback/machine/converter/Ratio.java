// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.converter;

import net.minecraft.util.StringRepresentable;

/** The named band steps of a solid-state converter (grid spec 5): a small set, never a free dial. A band is a packet four times the one below, so each step is a power of four in packet size. */
public enum Ratio implements StringRepresentable {
    DIV4("div4", -1),
    DIV16("div16", -2),
    MUL4("mul4", 1);

    private final String name;
    private final int bands;

    Ratio(String name, int bands) {
        this.name = name;
        this.bands = bands;
    }

    /** Bands the output sits above (positive) or below (negative) the input. */
    public int bands() {
        return bands;
    }

    /** The band this step makes of {@code in}, or null if it would fall off either end of the list. */
    public io.github.soundgoodizerfan.feedback.core.unit.VoltageBand out(io.github.soundgoodizerfan.feedback.core.unit.VoltageBand in) {
        int at = in.ordinal() + bands;
        var all = io.github.soundgoodizerfan.feedback.core.unit.VoltageBand.values();
        return at < 0 || at >= all.length ? null : all[at];
    }

    public Ratio next() {
        return values()[(ordinal() + 1) % values().length];
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
