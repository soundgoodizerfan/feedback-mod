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
 * 2 number in, 1 number out: philosophy 13's {@code + - * /}, with min and max beside them.
 * Shaped like {@link ComparatorNode} -- {@code rightInput} unwired reads {@code literalDefault} --
 * so a steering ratio is a seat key times a printed number.
 * <p>
 * A number reaching an actuator is a dial, and a factory controller is a switch (philosophy 13):
 * a numeric output only moves an {@link io.github.soundgoodizerfan.feedback.control.Analog}
 * target, and only vehicle parts are analog ({@code feedback_vehicles_spec.md} section 5,
 * <em>Controls</em>). Arithmetic itself is not the exception -- it was always on the list.
 */
public record MathNode(int id, Op op, int leftInput, int rightInput, float literalDefault) implements ProgramNode {

    public static final String TYPE = "math";

    public enum Op implements StringRepresentable {
        ADD("add"), SUBTRACT("subtract"), MULTIPLY("multiply"), DIVIDE("divide"), MIN("min"), MAX("max");

        private final String key;

        Op(String key) {
            this.key = key;
        }

        @Override
        public String getSerializedName() {
            return key;
        }

        public Op next() {
            return values()[(ordinal() + 1) % values().length];
        }

        /** Division by zero reads zero: a card has no way to report an error at run time. */
        public float apply(float a, float b) {
            return switch (this) {
                case ADD -> a + b;
                case SUBTRACT -> a - b;
                case MULTIPLY -> a * b;
                case DIVIDE -> b == 0 ? 0 : a / b;
                case MIN -> Math.min(a, b);
                case MAX -> Math.max(a, b);
            };
        }
    }

    public static final MapCodec<MathNode> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(MathNode::id),
            StringRepresentable.fromEnum(Op::values).fieldOf("op").forGetter(MathNode::op),
            Codec.INT.fieldOf("left_input").forGetter(MathNode::leftInput),
            Codec.INT.fieldOf("right_input").forGetter(MathNode::rightInput),
            Codec.FLOAT.fieldOf("literal_default").forGetter(MathNode::literalDefault)
    ).apply(instance, MathNode::new));

    public static final StreamCodec<ByteBuf, MathNode> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MathNode::id,
            ByteBufCodecs.idMapper(ByIdMap.continuous(Op::ordinal, Op.values(), ByIdMap.OutOfBoundsStrategy.ZERO), Op::ordinal), MathNode::op,
            ByteBufCodecs.VAR_INT, MathNode::leftInput,
            ByteBufCodecs.VAR_INT, MathNode::rightInput,
            ByteBufCodecs.FLOAT, MathNode::literalDefault,
            MathNode::new);

    @Override
    public String typeKey() {
        return TYPE;
    }
}
