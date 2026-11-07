// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
