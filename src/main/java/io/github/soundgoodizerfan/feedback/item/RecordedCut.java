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
package io.github.soundgoodizerfan.feedback.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * The one operation a recipe data stick holds: what was made and out of what class of stock.
 *
 * <p>{@code station} is the {@code modular/Station} id of the hand bench it was recorded at. Only the stonecutter records today; the
 * woodworking and smithing tables record the same way later, with their own station string and the same fields, so the format
 * is general and needs no change then. {@code result} is the item id the cut made and {@code resultForm} its form name when it has one
 * ({@code slab}), so a stick recorded on one stone's slab also cuts another stone's slab. {@code kind} and {@code form} are the input's
 * stock class: a stick only applies to stock of the class it was recorded on.
 */
public record RecordedCut(String station, String kind, String form, String result, String resultForm) {

    public static final Codec<RecordedCut> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("station").forGetter(RecordedCut::station),
            Codec.STRING.fieldOf("kind").forGetter(RecordedCut::kind),
            Codec.STRING.fieldOf("form").forGetter(RecordedCut::form),
            Codec.STRING.fieldOf("result").forGetter(RecordedCut::result),
            Codec.STRING.optionalFieldOf("result_form", "").forGetter(RecordedCut::resultForm)
    ).apply(i, RecordedCut::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, RecordedCut> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, RecordedCut::station,
            ByteBufCodecs.STRING_UTF8, RecordedCut::kind,
            ByteBufCodecs.STRING_UTF8, RecordedCut::form,
            ByteBufCodecs.STRING_UTF8, RecordedCut::result,
            ByteBufCodecs.STRING_UTF8, RecordedCut::resultForm,
            RecordedCut::new);
}
