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

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.StringRepresentable;

/**
 * 2 boolean in, 1 boolean out -- spec §2.2's logic-gate shape, covering {@code AND} and
 * {@code OR} through {@link Gate}, the way {@link ComparatorNode} covers Greater and Less. Spec
 * §2.3: a boolean input has no sensible default, so both inputs are mandatory wires --
 * {@link ProgramNode#UNWIRED} on a freshly placed card, and refused at print time until both
 * are wired.
 * <p>
 * {@code OR} is here because a deadband needs it: <em>on below the low mark, or already on and
 * still under the high mark</em>. The "already on" half is the actuator's own state read back
 * through a {@link SensorNode} -- state in the world, not in the program, so spec §1's ceiling
 * holds. Was {@code AndNode}; the type key stays {@code "and"} and a card printed before the op
 * existed reads as AND.
 */
public record LogicNode(int id, Gate op, int leftInput, int rightInput) implements ProgramNode {

    public static final String TYPE = "and";

    public enum Gate implements StringRepresentable {
        AND("and"),
        OR("or");

        private final String key;

        Gate(String key) {
            this.key = key;
        }

        @Override
        public String getSerializedName() {
            return key;
        }
    }

    public static final MapCodec<LogicNode> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(LogicNode::id),
            StringRepresentable.fromEnum(Gate::values).optionalFieldOf("op", Gate.AND).forGetter(LogicNode::op),
            Codec.INT.fieldOf("left_input").forGetter(LogicNode::leftInput),
            Codec.INT.fieldOf("right_input").forGetter(LogicNode::rightInput)
    ).apply(instance, LogicNode::new));

    public static final StreamCodec<ByteBuf, LogicNode> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LogicNode::id,
            ByteBufCodecs.idMapper(ByIdMap.continuous(Gate::ordinal, Gate.values(), ByIdMap.OutOfBoundsStrategy.ZERO), Gate::ordinal), LogicNode::op,
            ByteBufCodecs.VAR_INT, LogicNode::leftInput,
            ByteBufCodecs.VAR_INT, LogicNode::rightInput,
            LogicNode::new);

    @Override
    public String typeKey() {
        return TYPE;
    }
}
