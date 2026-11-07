// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.converter;

import net.minecraft.util.StringRepresentable;

/** The named voltage ratios of a solid-state converter (grid spec 5): a small set, never a free dial. Output volts over input volts. */
public enum Ratio implements StringRepresentable {
    DIV4("div4", 0.25f),
    DIV16("div16", 1f / 16f),
    MUL4("mul4", 4f);

    private final String name;
    private final float factor;

    Ratio(String name, float factor) {
        this.name = name;
        this.factor = factor;
    }

    public float factor() {
        return factor;
    }

    public Ratio next() {
        return values()[(ordinal() + 1) % values().length];
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
