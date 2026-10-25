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

import java.util.HashMap;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * What a ladled serving is called, whether anything live is left in it, and what its buffs are
 * (food spec 7): compound strengths as concentrations, and the fat and fibre that slow them. Its
 * hunger, saturation and hazards are its {@code food} component, baked at the ladle.
 */
public record Meal(String dish, float pathogen, Map<String, Float> compounds, float fat, float fibre) {

    public static final Codec<Meal> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("dish").forGetter(Meal::dish),
            Codec.FLOAT.fieldOf("pathogen").forGetter(Meal::pathogen),
            Codec.unboundedMap(Codec.STRING, Codec.FLOAT).optionalFieldOf("compounds", Map.of()).forGetter(Meal::compounds),
            Codec.FLOAT.optionalFieldOf("fat", 0f).forGetter(Meal::fat),
            Codec.FLOAT.optionalFieldOf("fibre", 0f).forGetter(Meal::fibre)
    ).apply(instance, Meal::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Meal> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, Meal::dish,
            ByteBufCodecs.FLOAT, Meal::pathogen,
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.FLOAT), Meal::compounds,
            ByteBufCodecs.FLOAT, Meal::fat,
            ByteBufCodecs.FLOAT, Meal::fibre,
            Meal::new);
}
