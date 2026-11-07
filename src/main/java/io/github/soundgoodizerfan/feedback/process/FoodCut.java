// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
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
