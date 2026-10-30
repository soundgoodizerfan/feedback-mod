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

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * How a food was cut at the Prep Table: absent means whole.
 * Both fields are classes, never a measurement: {@link io.github.soundgoodizerfan.feedback.modular.PrepWork} grades them.
 *
 * @param level 1 sliced, 2 minced (0 is whole and is stored as no component)
 * @param grade 0 even, 1 mixed, 2 ragged: how alike the pieces came out
 */
public record FoodCut(int level, int grade) {

    public static final int EVEN = 0, MIXED = 1, RAGGED = 2;

    public static final Codec<FoodCut> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(FoodCut::level),
            Codec.INT.fieldOf("grade").forGetter(FoodCut::grade)
    ).apply(instance, FoodCut::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FoodCut> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, FoodCut::level,
            ByteBufCodecs.VAR_INT, FoodCut::grade,
            FoodCut::new);

    public boolean ragged() {
        return level > 0 && grade == RAGGED;
    }
}
