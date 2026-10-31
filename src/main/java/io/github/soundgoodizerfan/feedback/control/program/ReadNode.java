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
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * No input, 1 output of the named {@link ProgramVariable}'s type: the value it held at the start
 * of this step. A state node, so a wire cycle through it is legal.
 */
public record ReadNode(int id, String variable) implements ProgramNode {

    public static final String TYPE = "read";

    public static final MapCodec<ReadNode> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(ReadNode::id),
            Codec.string(0, FTuning.VARIABLE_NAME_MAX).fieldOf("variable").forGetter(ReadNode::variable)
    ).apply(instance, ReadNode::new));

    public static final StreamCodec<ByteBuf, ReadNode> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ReadNode::id,
            ByteBufCodecs.stringUtf8(FTuning.VARIABLE_NAME_MAX), ReadNode::variable,
            ReadNode::new);

    @Override
    public String typeKey() {
        return TYPE;
    }
}
