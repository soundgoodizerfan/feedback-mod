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
