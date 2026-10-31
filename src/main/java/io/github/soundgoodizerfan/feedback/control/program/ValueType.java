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
package io.github.soundgoodizerfan.feedback.control.program;

import com.mojang.serialization.Codec;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

/** The two kinds of signal a wire carries: a number (a reading, a count) or a boolean. */
public enum ValueType implements StringRepresentable {
    NUMBER("number"), BOOLEAN("boolean");

    public static final Codec<ValueType> CODEC = StringRepresentable.fromEnum(ValueType::values);

    public static final StreamCodec<ByteBuf, ValueType> STREAM_CODEC = ByteBufCodecs.idMapper(
            ByIdMap.continuous(ValueType::ordinal, values(), ByIdMap.OutOfBoundsStrategy.ZERO), ValueType::ordinal);

    private final String key;

    ValueType(String key) {
        this.key = key;
    }

    @Override
    public String getSerializedName() {
        return key;
    }

    /** What a variable of this type holds before anything is written: 0 or false. */
    public Object zero() {
        return this == NUMBER ? (Object) 0f : (Object) Boolean.FALSE;
    }

    public ValueType next() {
        return values()[(ordinal() + 1) % values().length];
    }
}
