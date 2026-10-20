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
 */
public record MadeOf(ResourceLocation material, String stock, int cells) {

    public static final Codec<MadeOf> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("material").forGetter(MadeOf::material),
            Codec.STRING.fieldOf("stock").forGetter(MadeOf::stock),
            Codec.INT.fieldOf("cells").forGetter(MadeOf::cells)
    ).apply(i, MadeOf::new));

    public static final StreamCodec<ByteBuf, MadeOf> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, MadeOf::material,
            ByteBufCodecs.STRING_UTF8, MadeOf::stock,
            ByteBufCodecs.VAR_INT, MadeOf::cells,
            MadeOf::new);
}
