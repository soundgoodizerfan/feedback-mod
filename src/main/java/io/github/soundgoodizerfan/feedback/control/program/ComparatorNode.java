// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.program;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ByIdMap;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * 2 number in, 1 boolean out -- spec §2.2's comparator shape, covering both {@code Greater Than}
 * and {@code Less Than} (§5) through {@link Compare}.
 * <p>
 * {@code rightInput} is the port §2.3 describes with an inline default: {@link ProgramNode#UNWIRED}
 * (nothing wired, including on a freshly placed card) reads {@code literalDefault} instead.
 * {@code leftInput} has no such fallback -- it starts {@code UNWIRED} too, but that is simply an
 * incomplete card, refused at print time by {@link ProgramGraph#findError}.
 */
public record ComparatorNode(int id, Compare op, int leftInput, int rightInput,
                              float literalDefault) implements ProgramNode {

    public static final String TYPE = "comparator";

    public enum Compare implements StringRepresentable {
        GREATER("greater"),
        LESS("less");

        private final String key;

        Compare(String key) {
            this.key = key;
        }

        @Override
        public String getSerializedName() {
            return key;
        }
    }

    public static final MapCodec<ComparatorNode> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("id").forGetter(ComparatorNode::id),
            StringRepresentable.fromEnum(Compare::values).fieldOf("op").forGetter(ComparatorNode::op),
            Codec.INT.fieldOf("left_input").forGetter(ComparatorNode::leftInput),
            Codec.INT.fieldOf("right_input").forGetter(ComparatorNode::rightInput),
            Codec.FLOAT.fieldOf("literal_default").forGetter(ComparatorNode::literalDefault)
    ).apply(instance, ComparatorNode::new));

    public static final StreamCodec<ByteBuf, ComparatorNode> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ComparatorNode::id,
            ByteBufCodecs.idMapper(ByIdMap.continuous(Compare::ordinal, Compare.values(), ByIdMap.OutOfBoundsStrategy.ZERO), Compare::ordinal), ComparatorNode::op,
            ByteBufCodecs.VAR_INT, ComparatorNode::leftInput,
            ByteBufCodecs.VAR_INT, ComparatorNode::rightInput,
            ByteBufCodecs.FLOAT, ComparatorNode::literalDefault,
            ComparatorNode::new);

    @Override
    public String typeKey() {
        return TYPE;
    }
}
