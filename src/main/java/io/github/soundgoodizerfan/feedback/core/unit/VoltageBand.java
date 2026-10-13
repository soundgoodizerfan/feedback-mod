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
    LV, MV, HV, UHV;

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
