// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * What a pair of tongs is made of: the metal of its arms and the stuff of its grip.
 *
 * <p>The arms must not melt where they meet the piece; the grip must not burn where the hand
 * meets it. Two materials, two limits, and the item has no number of its own.
 */
public record Tongs(ResourceLocation arms, ResourceLocation grip) {

    public static final Codec<Tongs> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("arms").forGetter(Tongs::arms),
            ResourceLocation.CODEC.fieldOf("grip").forGetter(Tongs::grip)
    ).apply(i, Tongs::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Tongs> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, Tongs::arms,
            ResourceLocation.STREAM_CODEC, Tongs::grip,
            Tongs::new);
}
