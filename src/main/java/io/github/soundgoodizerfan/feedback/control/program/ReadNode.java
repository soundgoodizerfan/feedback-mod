// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.program;

import io.github.soundgoodizerfan.feedback.control.ControlTuning;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

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
            Codec.string(0, ControlTuning.VARIABLE_NAME_MAX).fieldOf("variable").forGetter(ReadNode::variable)
    ).apply(instance, ReadNode::new));

    public static final StreamCodec<ByteBuf, ReadNode> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ReadNode::id,
            ByteBufCodecs.stringUtf8(ControlTuning.VARIABLE_NAME_MAX), ReadNode::variable,
            ReadNode::new);

    @Override
    public String typeKey() {
        return TYPE;
    }
}
