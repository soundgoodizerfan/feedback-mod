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

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.github.soundgoodizerfan.feedback.control.data.DataNodeRef;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import org.jetbrains.annotations.Nullable;

/**
 * 1 boolean in, nothing out -- a terminal card. In a vehicle the input may be a number instead,
 * and then it drives an {@link io.github.soundgoodizerfan.feedback.control.Analog} part. Names the {@link DataNodeRef} of the
 * {@code Switchable}/{@code DataNode} it drives, embedded the same way {@link SensorNode} embeds
 * its target (spec §3 and §5's {@code Flip Clutch}) -- null until a Programmer's "Link" fills it
 * in. {@code input} is {@link ProgramNode#UNWIRED} until wired to whatever feeds its condition.
 */
public record ActuatorNode(int id, int input, @Nullable DataNodeRef target, int channel) implements ProgramNode {

    public static final String TYPE = "actuator";

    /**
     * Which of an {@link io.github.soundgoodizerfan.feedback.control.Analog} target's inputs a number
     * drives -- a chassis's steering or its brakes. Zero for a switch, and for an analog part with
     * one input (an engine's throttle).
     */
    public ActuatorNode(int id, int input, @Nullable DataNodeRef target) {
        this(id, input, target, 0);
    }

    public ActuatorNode withChannel(int channel) {
        return new ActuatorNode(id, input, target, channel);
    }

    public static final MapCodec<ActuatorNode> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(ActuatorNode::id),
            Codec.INT.fieldOf("input").forGetter(ActuatorNode::input),
            DataNodeRef.CODEC.optionalFieldOf("target").forGetter(n -> Optional.ofNullable(n.target())),
            Codec.INT.optionalFieldOf("channel", 0).forGetter(ActuatorNode::channel)
    ).apply(instance, (id, input, target, channel) -> new ActuatorNode(id, input, target.orElse(null), channel)));

    public static final StreamCodec<ByteBuf, ActuatorNode> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ActuatorNode::id,
            ByteBufCodecs.VAR_INT, ActuatorNode::input,
            ByteBufCodecs.optional(DataNodeRef.STREAM_CODEC), n -> Optional.ofNullable(n.target()),
            ByteBufCodecs.VAR_INT, ActuatorNode::channel,
            (id, input, target, channel) -> new ActuatorNode(id, input, target.orElse(null), channel));

    @Override
    public String typeKey() {
        return TYPE;
    }
}
