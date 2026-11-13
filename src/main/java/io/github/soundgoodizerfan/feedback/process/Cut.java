// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import com.mojang.serialization.Codec;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.registry.FDataComponents;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

/**
 * How far a crystal plate has been ground: a data component on the plate, carried
 * onto a mass probe made from it. A plate with no component is the first cut, coarse. Grinding a
 * cut plate moves it one cut finer ({@code finer} form step); the finest has nowhere further to go.
 * A thin cut reads fine and not far, a thick one far and coarse.
 */
public enum Cut implements StringRepresentable {
    COARSE("coarse", FTuning.MASS_PROBE_RANGE_COARSE_QU, FTuning.MASS_PROBE_STEP_COARSE_QU),
    STANDARD("standard", FTuning.MASS_PROBE_RANGE_STANDARD_QU, FTuning.MASS_PROBE_STEP_STANDARD_QU),
    FINE("fine", FTuning.MASS_PROBE_RANGE_FINE_QU, FTuning.MASS_PROBE_STEP_FINE_QU);

    public static final Codec<Cut> CODEC = StringRepresentable.fromEnum(Cut::values);
    public static final StreamCodec<ByteBuf, Cut> STREAM_CODEC =
            ByteBufCodecs.idMapper(i -> values()[i], Cut::ordinal);

    private final String key;
    /** The most coat a probe of this cut reads, and the smallest step it tells apart, in Qu. */
    public final float range;
    public final float step;

    Cut(String key, float range, float step) {
        this.key = key;
        this.range = range;
        this.step = step;
    }

    public Cut finer() {
        return values()[Math.min(ordinal() + 1, values().length - 1)];
    }

    public String translationKey() {
        return "feedback.cut." + key;
    }

    /** The cut on a stack: its component, else the first cut. */
    public static Cut of(ItemStack stack) {
        return stack.getOrDefault(FDataComponents.CUT.get(), COARSE);
    }

    @Override
    public String getSerializedName() {
        return key;
    }
}
