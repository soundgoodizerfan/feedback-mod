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
 * 1 in, 1 out of the same {@link ValueType}: the output is the input as it was {@code ticks}
 * ago. A state node: its output is history, so a wire cycle through it is legal (a latch, an
 * oscillator). {@code ticks} is one of {@link FTuning#DELAY_NAMED_TICKS} on any Controller;
 * any other count from {@link FTuning#DELAY_TYPED_MIN_TICKS} up is a typed delay and needs an
 * Escapement fitted to the Controller the card runs in.
 */
public record DelayNode(int id, int input, ValueType type, int ticks) implements ProgramNode {

    public static final String TYPE = "delay";

    public static final MapCodec<DelayNode> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(DelayNode::id),
            Codec.INT.fieldOf("input").forGetter(DelayNode::input),
            ValueType.CODEC.fieldOf("value_type").forGetter(DelayNode::type),
            Codec.intRange(1, FTuning.DELAY_MAX_TICKS).fieldOf("ticks").forGetter(DelayNode::ticks)
    ).apply(instance, DelayNode::new));

    public static final StreamCodec<ByteBuf, DelayNode> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DelayNode::id,
            ByteBufCodecs.VAR_INT, DelayNode::input,
            ValueType.STREAM_CODEC, DelayNode::type,
            ByteBufCodecs.VAR_INT, DelayNode::ticks,
            DelayNode::new);

    /** Whether {@link #ticks} is on the named list (any Controller) rather than a typed count. */
    public boolean named() {
        for (int t : FTuning.DELAY_NAMED_TICKS)
            if (t == ticks)
                return true;
        return false;
    }

    @Override
    public String typeKey() {
        return TYPE;
    }
}
