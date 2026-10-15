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
