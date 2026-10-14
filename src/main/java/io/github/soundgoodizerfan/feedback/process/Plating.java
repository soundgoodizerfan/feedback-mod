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
package io.github.soundgoodizerfan.feedback.process;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;

/**
 * A coat of metal electroplated onto an item (mechanics 6.15). The metal is the coat's dominant
 * one, the thickness is how much of it there is in Qu (internal, no type, as the cell's own ledger
 * holds it), and the quality is what went right or wrong while it grew. A coat too thick to hold
 * never becomes one of these: it flakes off as it comes out of the water.
 * <p>
 * The item is still whatever it was. Plating changes no identity -- {@code Material.find} still
 * calls a plated iron ingot iron -- which is what will make a gold-plated one fool an optical
 * sorter on purpose (philosophy 11) once one exists.
 */
public record Plating(ResourceLocation metal, float thickness, Quality quality) {

    /** The severity ladder (philosophy 6), as a coat reaches it. */
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
            Quality.CODEC.fieldOf("quality").forGetter(Plating::quality)
    ).apply(i, Plating::new));

    public static final StreamCodec<ByteBuf, Plating> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, Plating::metal,
            ByteBufCodecs.FLOAT, Plating::thickness,
            ByteBufCodecs.idMapper(i -> Quality.values()[i], Quality::ordinal), Plating::quality,
            Plating::new);
}
