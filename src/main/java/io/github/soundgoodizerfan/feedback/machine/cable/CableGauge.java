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
package io.github.soundgoodizerfan.feedback.machine.cable;

import java.util.Locale;

import com.mojang.serialization.Codec;

import io.github.soundgoodizerfan.feedback.core.FTuning;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * A cable's cross-section: four sizes, each four times the copper of the one before. Area is the
 * whole story (ampacity scales with it, volts lost per amp inversely), so a gauge is one multiplier
 * on the material's own figures. The order is the wire form's ordinal: append, never reorder.
 *
 * <p>{@code halfWidth} and {@code halfThick} (sixteenths of a block) are the look of the core and its
 * arms: round gauges are square, the bus bar is a flat bar wider than it is thick.
 */
public enum CableGauge implements StringRepresentable {
    THIN(FTuning.CABLE_GAUGE_THIN_AREA, 2, 2),
    STANDARD(FTuning.CABLE_GAUGE_STANDARD_AREA, 3, 3),
    HEAVY(FTuning.CABLE_GAUGE_HEAVY_AREA, 4, 4),
    BUS(FTuning.CABLE_GAUGE_BUS_AREA, 6, 2);

    public static final Codec<CableGauge> CODEC = StringRepresentable.fromEnum(CableGauge::values);
    public static final StreamCodec<ByteBuf, CableGauge> STREAM_CODEC =
            ByteBufCodecs.idMapper(i -> values()[i], CableGauge::ordinal);

    private final float area;
    private final int halfWidth, halfThick;

    CableGauge(float area, int halfWidth, int halfThick) {
        this.area = area;
        this.halfWidth = halfWidth;
        this.halfThick = halfThick;
    }

    /** Cross-section against the standard cable: rating multiplies by it, drop divides. */
    public float area() {
        return area;
    }

    public int halfWidth() {
        return halfWidth;
    }

    public int halfThick() {
        return halfThick;
    }

    /** Cables one craft of this gauge yields. */
    public int yield() {
        return switch (this) {
            case THIN -> FTuning.CABLE_GAUGE_THIN_YIELD;
            case STANDARD -> FTuning.CABLE_GAUGE_STANDARD_YIELD;
            case HEAVY -> FTuning.CABLE_GAUGE_HEAVY_YIELD;
            case BUS -> FTuning.CABLE_GAUGE_BUS_YIELD;
        };
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Translation key of the gauge's name. */
    public String key() {
        return "feedback.cable.gauge." + getSerializedName();
    }
}
