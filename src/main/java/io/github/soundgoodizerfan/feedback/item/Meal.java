// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a ladled serving is called, whether anything live is left in it, and what its buffs are
 * (food spec 7): compound strengths as concentrations, and the fat and fibre that slow them. {@code ingredients} is material path to share of the pot (0..1), for naming. Its
 * hunger, saturation and hazards are its {@code food} component, baked at the ladle.
 */
public record Meal(String dish, float pathogen, Map<String, Float> compounds, float fat, float fibre, Map<String, Float> ingredients) {

    public static final Codec<Meal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("dish").forGetter(Meal::dish),
            Codec.FLOAT.fieldOf("pathogen").forGetter(Meal::pathogen),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("compounds", Map.of()).forGetter(Meal::compounds),
            Codec.FLOAT.optionalFieldOf("fat", 0f).forGetter(Meal::fat),
            Codec.FLOAT.optionalFieldOf("fibre", 0f).forGetter(Meal::fibre),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("ingredients", Map.of()).forGetter(Meal::ingredients)
    ).apply(instance, Meal::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Meal> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Meal::dish,
            ByteBufCodecs.FLOAT, Meal::pathogen,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.FLOAT), Meal::compounds,
            ByteBufCodecs.FLOAT, Meal::fat,
            ByteBufCodecs.FLOAT, Meal::fibre,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.FLOAT), Meal::ingredients,
            Meal::new);
}
