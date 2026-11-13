// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.program;

import io.github.soundgoodizerfan.feedback.control.ControlTuning;
import java.util.regex.Pattern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * One named, typed memory cell a program declares. {@link ReadNode} and {@link WriteNode} cards
 * refer to it by name; the Controller keeps its value (numbers start at 0, booleans at false).
 */
public record ProgramVariable(String name, ValueType type) {

    private static final Pattern NAME = Pattern.compile("[\\p{L}\\p{N}_-]+( [\\p{L}\\p{N}_-]+)*");

    public static final Codec<ProgramVariable> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.string(0, ControlTuning.VARIABLE_NAME_MAX).fieldOf("name").forGetter(ProgramVariable::name),
            ValueType.CODEC.fieldOf("type").forGetter(ProgramVariable::type)
    ).apply(instance, ProgramVariable::new));

    public static final StreamCodec<ByteBuf, ProgramVariable> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(ControlTuning.VARIABLE_NAME_MAX), ProgramVariable::name,
            ValueType.STREAM_CODEC, ProgramVariable::type,
            ProgramVariable::new);

    /** Short, non-blank, single-spaced letters, digits, underscore and dash. */
    public static boolean validName(String name) {
        return name.length() <= ControlTuning.VARIABLE_NAME_MAX && NAME.matcher(name).matches();
    }
}
