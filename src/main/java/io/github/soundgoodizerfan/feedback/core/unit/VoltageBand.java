// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.core.unit;

import java.util.Locale;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * A voltage rating (mechanics 6.3): a closed, ordered list of named bands, geometrically spaced,
 * never a free scalar. Voltage belongs to the source, and a band gates nothing by itself -- a
 * mismatch is a consequence (6.5), not a lock.
 *
 * <p>The order below is the order of rising voltage and is part of the type: the wire form is the
 * ordinal, so append a new band at the top and never reorder. The disk form is the lowercase
 * name, so a pack never depends on the ordinal.
 */
public enum VoltageBand implements StringRepresentable {
    LV, MV, HV, UHV, EHV, TRUNK;

    public static final Codec<VoltageBand> CODEC = StringRepresentable.fromEnum(VoltageBand::values);

    public static final StreamCodec<ByteBuf, VoltageBand> STREAM_CODEC =
            ByteBufCodecs.idMapper(i -> values()[i], VoltageBand::ordinal);

    @Override
    public String getSerializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Translation key of the band's plain name ({@code LV}, {@code MV}...). */
    public String key() {
        return "feedback.unit.eu.band." + getSerializedName();
    }
}
