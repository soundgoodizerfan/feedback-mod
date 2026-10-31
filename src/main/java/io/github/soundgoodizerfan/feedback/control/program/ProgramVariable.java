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

import java.util.regex.Pattern;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.core.FTuning;
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
            Codec.string(0, FTuning.VARIABLE_NAME_MAX).fieldOf("name").forGetter(ProgramVariable::name),
            ValueType.CODEC.fieldOf("type").forGetter(ProgramVariable::type)
    ).apply(instance, ProgramVariable::new));

    public static final StreamCodec<ByteBuf, ProgramVariable> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(FTuning.VARIABLE_NAME_MAX), ProgramVariable::name,
            ValueType.STREAM_CODEC, ProgramVariable::type,
            ProgramVariable::new);

    /** Short, non-blank, single-spaced letters, digits, underscore and dash. */
    public static boolean validName(String name) {
        return name.length() <= FTuning.VARIABLE_NAME_MAX && NAME.matcher(name).matches();
    }
}
