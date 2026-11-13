// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.cable;

import io.github.soundgoodizerfan.feedback.machine.ElectricMachineTuning;
import java.util.Locale;

import com.mojang.serialization.Codec;


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
    THIN(ElectricMachineTuning.CABLE_GAUGE_THIN_AREA, 2, 2),
    STANDARD(ElectricMachineTuning.CABLE_GAUGE_STANDARD_AREA, 3, 3),
    HEAVY(ElectricMachineTuning.CABLE_GAUGE_HEAVY_AREA, 4, 4),
    BUS(ElectricMachineTuning.CABLE_GAUGE_BUS_AREA, 6, 2);

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
            case THIN -> ElectricMachineTuning.CABLE_GAUGE_THIN_YIELD;
            case STANDARD -> ElectricMachineTuning.CABLE_GAUGE_STANDARD_YIELD;
            case HEAVY -> ElectricMachineTuning.CABLE_GAUGE_HEAVY_YIELD;
            case BUS -> ElectricMachineTuning.CABLE_GAUGE_BUS_YIELD;
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
