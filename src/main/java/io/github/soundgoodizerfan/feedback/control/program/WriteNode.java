// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.program;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Terminal with two inputs: a value of the named {@link ProgramVariable}'s type (port 0) and a
 * boolean trigger (port 1). The step the trigger is true the variable takes the value; otherwise
 * it holds. A {@link ReadNode} sees the new value from the next step.
 */
public record WriteNode(int id, String variable, int valueInput, int triggerInput) implements ProgramNode {

    public static final String TYPE = "write";

    public static final MapCodec<WriteNode> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(WriteNode::id),
            Codec.string(0, FTuning.VARIABLE_NAME_MAX).fieldOf("variable").forGetter(WriteNode::variable),
            Codec.INT.fieldOf("value_input").forGetter(WriteNode::valueInput),
            Codec.INT.fieldOf("trigger_input").forGetter(WriteNode::triggerInput)
    ).apply(instance, WriteNode::new));

    public static final StreamCodec<ByteBuf, WriteNode> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WriteNode::id,
            ByteBufCodecs.stringUtf8(FTuning.VARIABLE_NAME_MAX), WriteNode::variable,
            ByteBufCodecs.VAR_INT, WriteNode::valueInput,
            ByteBufCodecs.VAR_INT, WriteNode::triggerInput,
            WriteNode::new);

    @Override
    public String typeKey() {
        return TYPE;
    }
}
