// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import io.github.soundgoodizerfan.feedback.core.FTuning;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a dish was baked to, before time touched it: the figures
 * {@link Cooking#apply} scales by the dish's {@link FoodState}, so spoiling again and again never compounds.
 *
 * @param hunger     authored hunger points
 * @param saturation authored saturation
 * @param eatSeconds the eating time (the container's, not spoilage's)
 * @param moisture   the blended water the dish was baked at: drying is measured against it
 * @param spilled    0..1: how much of the wettest part has been lost to being carried overfull (hunger and saturation already net of it)
 * @param overflow   mB of food over the container's volume, 0 when it fits: what carrying spills from
 * @param seal       the container's seal, 0 open, 1 covered, 2 sealed
 */
public record DishFigures(float hunger, float saturation, float eatSeconds, float moisture, float spilled, float overflow, int seal) {

    public static final Codec<DishFigures> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("hunger").forGetter(DishFigures::hunger),
            Codec.FLOAT.fieldOf("saturation").forGetter(DishFigures::saturation),
            Codec.FLOAT.fieldOf("eat_seconds").forGetter(DishFigures::eatSeconds),
            Codec.FLOAT.fieldOf("moisture").forGetter(DishFigures::moisture),
            Codec.FLOAT.optionalFieldOf("spilled", 0f).forGetter(DishFigures::spilled),
            Codec.FLOAT.optionalFieldOf("overflow", 0f).forGetter(DishFigures::overflow),
            Codec.INT.optionalFieldOf("seal", 0).forGetter(DishFigures::seal)
    ).apply(instance, DishFigures::new));

    // Seven fields are past StreamCodec.composite's six.
    public static final StreamCodec<RegistryFriendlyByteBuf, DishFigures> STREAM_CODEC = StreamCodec.of(
            (buf, d) -> {
                buf.writeFloat(d.hunger);
                buf.writeFloat(d.saturation);
                buf.writeFloat(d.eatSeconds);
                buf.writeFloat(d.moisture);
                buf.writeFloat(d.spilled);
                buf.writeFloat(d.overflow);
                buf.writeVarInt(d.seal);
            },
            buf -> new DishFigures(buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt()));

    /** How much of the wettest part the overflow could take at most: the spill stops there. */
    public float room() {
        return Math.min(1f, overflow / io.github.soundgoodizerfan.feedback.core.FTuning.DISH_PART_MB);
    }

    /** Whether carrying it still loses food. */
    public boolean spilling() {
        return spilled < room();
    }

    /** What a seal leaves of drying, migration and spill: open 1, covered half, sealed none. */
    public static float sealScale(int seal) {
        return Math.max(0f, 1f - io.github.soundgoodizerfan.feedback.process.ProcessTuning.SEAL_SCALE_PER_LEVEL * seal);
    }
}
