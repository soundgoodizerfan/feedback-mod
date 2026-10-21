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
 * @param dims     as-built measures a machine left on it, by the names its role declares
 *                 ({@link io.github.soundgoodizerfan.feedback.modular.ModularType.Dim}): a barrel's bore, outside and
 *                 length. Empty for a part whose role declares none.
 */
public record MadeOf(ResourceLocation material, String stock, int cells, String variant, int flaw,
                     java.util.Map<String, Float> dims) {

    public MadeOf {
        dims = java.util.Map.copyOf(dims);
    }

    public MadeOf(ResourceLocation material, String stock, int cells) {
        this(material, stock, cells, "", 0, java.util.Map.of());
    }

    public MadeOf(ResourceLocation material, String stock, int cells, String variant) {
        this(material, stock, cells, variant, 0, java.util.Map.of());
    }

    public MadeOf(ResourceLocation material, String stock, int cells, String variant, int flaw) {
        this(material, stock, cells, variant, flaw, java.util.Map.of());
    }

    /** This part with the measure {@code name} set: what a drill, a lathe or a cutoff wheel leaves on it. */
    public MadeOf withDim(String name, float value) {
        java.util.Map<String, Float> out = new java.util.HashMap<>(dims);
        out.put(name, value);
        return new MadeOf(material, stock, cells, variant, flaw, out);
    }

    public static final Codec<MadeOf> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("material").forGetter(MadeOf::material),
            Codec.STRING.fieldOf("stock").forGetter(MadeOf::stock),
            Codec.INT.fieldOf("cells").forGetter(MadeOf::cells),
            Codec.STRING.optionalFieldOf("variant", "").forGetter(MadeOf::variant),
            Codec.INT.optionalFieldOf("flaw", 0).forGetter(MadeOf::flaw),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("dims", java.util.Map.of()).forGetter(MadeOf::dims)
    ).apply(i, MadeOf::new));

    public static final StreamCodec<ByteBuf, MadeOf> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, MadeOf::material,
            ByteBufCodecs.STRING_UTF8, MadeOf::stock,
            ByteBufCodecs.VAR_INT, MadeOf::cells,
            ByteBufCodecs.STRING_UTF8, MadeOf::variant,
            ByteBufCodecs.VAR_INT, MadeOf::flaw,
            ByteBufCodecs.map(java.util.HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.FLOAT), MadeOf::dims,
            MadeOf::new);
}
