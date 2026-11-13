// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * A coat of metal electroplated onto an item. The metal is the coat's dominant
 * one, the thickness is how much of it there is in Qu (internal, no type, as the cell's own ledger
 * holds it), the quality is what went right or wrong while it grew, and {@code intact} is the share
 * of the coat still on the piece (0 to 1): a brittle coat (fired enamel) loses it to hard hits, a
 * firing restores it, and what the coat protects scales with it. A coat too thick to hold
 * never becomes one of these: it flakes off as it comes out of the water.
 * <p>
 * The item is still whatever it was. Plating changes no identity -- {@code Material.find} still
 * calls a plated iron ingot iron -- which is what will make a gold-plated one fool an optical
 * sorter on purpose (philosophy 12) once one exists.
 */
public record Plating(ResourceLocation metal, float thickness, Quality quality, float intact) {

    /** A whole coat: nothing has chipped off it. */
    public Plating(ResourceLocation metal, float thickness, Quality quality) {
        this(metal, thickness, quality, 1f);
    }

    /** The same coat with {@code share} (0 to 1) of what it was left. */
    public Plating withIntact(float share) {
        return new Plating(metal, thickness, quality, Math.max(0f, Math.min(1f, share)));
    }

    /** The severity ladder (philosophy 7), as a coat reaches it. */
    public enum Quality implements StringRepresentable {
        /** In the window: thick enough to close, thin enough to hold. */
        SOUND,
        /** Taken out early: the coat has not closed over the piece. Degrade. */
        POROUS,
        /** Grown at too much current for the piece's size: rough and dark. Degrade. */
        BURNT,
        /** More than one metal came out together: the voltage reached a second one. Degrade. */
        MIXED;

        public static final Codec<Quality> CODEC = StringRepresentable.fromEnum(Quality::values);

        @Override
        public String getSerializedName() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public static final Codec<Plating> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("metal").forGetter(Plating::metal),
            Codec.FLOAT.fieldOf("thickness").forGetter(Plating::thickness),
            Quality.CODEC.fieldOf("quality").forGetter(Plating::quality),
            Codec.floatRange(0f, 1f).optionalFieldOf("intact", 1f).forGetter(Plating::intact)
    ).apply(i, Plating::new));

    public static final StreamCodec<ByteBuf, Plating> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, Plating::metal,
            ByteBufCodecs.FLOAT, Plating::thickness,
            ByteBufCodecs.idMapper(i -> Quality.values()[i], Quality::ordinal), Plating::quality,
            ByteBufCodecs.FLOAT, Plating::intact,
            Plating::new);
}
