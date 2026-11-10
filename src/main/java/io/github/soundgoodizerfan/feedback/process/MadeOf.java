// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.process;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * What a shaped part is made of, carried on the stack: one item per <em>kind</em> of part
 * ({@code feedback:pickaxe_head}) and its material here, rather than one generated item per
 * material and part ({@code iron_pickaxe_head}, {@code copper_pickaxe_head}, ...). The form system
 * mints an item per pair because a plate is a stock form a dozen systems read by item; a part is
 * read by nothing but what it is made of, so the pair belongs in data. {@link Material#find} reads
 * this before the item, so a part is found by its material exactly as an ingot is (philosophy 11).
 *
 * @param material the material it was shaped from.
 * @param stock    the form that went in (ingot, gem, chain): what a role weighs by, and what mends
 *                 it.
 * @param cells    how many of that stock went in, which is its size.
 * @param variant  which of the part's shapes it was made in ({@link io.github.soundgoodizerfan.feedback.modular.ModularType.Variant});
 *                 empty for the standard one.
 * @param flaw     how badly it was forged, 0 (clean) to 3: each step costs the edge toughness
 *                 ({@code ForgeRhythm}). A flawed part is never refused, only weaker.
 * @param measures as-built axes, forge work, attunements and live state ({@link Measures}).
 */
public record MadeOf(ResourceLocation material, String stock, int cells, String variant, int flaw, Measures measures) {

    public MadeOf(ResourceLocation material, String stock, int cells) {
        this(material, stock, cells, "", 0, Measures.NONE);
    }

    public MadeOf(ResourceLocation material, String stock, int cells, String variant) {
        this(material, stock, cells, variant, 0, Measures.NONE);
    }

    public MadeOf(ResourceLocation material, String stock, int cells, String variant, int flaw) {
        this(material, stock, cells, variant, flaw, Measures.NONE);
    }

    /** This part with the as-built axis {@code name} set: what a drill, a lathe or a cutoff wheel leaves on it. */
    public MadeOf withAxis(String name, float value) {
        return withMeasures(measures.withAxis(name, value));
    }

    public MadeOf withMeasures(Measures m) {
        return new MadeOf(material, stock, cells, variant, flaw, m);
    }

    public static final Codec<MadeOf> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("material").forGetter(MadeOf::material),
            Codec.STRING.fieldOf("stock").forGetter(MadeOf::stock),
            Codec.INT.fieldOf("cells").forGetter(MadeOf::cells),
            Codec.STRING.optionalFieldOf("variant", "").forGetter(MadeOf::variant),
            Codec.INT.optionalFieldOf("flaw", 0).forGetter(MadeOf::flaw),
            Measures.CODEC.optionalFieldOf("measures", Measures.NONE).forGetter(MadeOf::measures)
    ).apply(i, MadeOf::new));

    public static final StreamCodec<ByteBuf, MadeOf> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, MadeOf::material,
            ByteBufCodecs.STRING_UTF8, MadeOf::stock,
            ByteBufCodecs.VAR_INT, MadeOf::cells,
            ByteBufCodecs.STRING_UTF8, MadeOf::variant,
            ByteBufCodecs.VAR_INT, MadeOf::flaw,
            Measures.STREAM_CODEC, MadeOf::measures,
            MadeOf::new);
}
