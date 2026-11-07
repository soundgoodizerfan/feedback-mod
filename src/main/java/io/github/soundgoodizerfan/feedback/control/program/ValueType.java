// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
