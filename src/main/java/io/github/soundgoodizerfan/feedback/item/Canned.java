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
import net.minecraft.world.item.ItemStack;

/**
 * What is in a jar and how far the Pressure Vessel has taken it (food spec 8, Canning).
 *
 * @param food   the one food or serving inside, with its own components; a sealed jar never reads its spoilage clock.
 * @param sterile 0 to 1: heat held above the sterilising point. At 1 the jar seals.
 * @param sealed  sterilised and closed: nothing live in it, and it does not rot.
 * @param mush    0 to 1: heat kept on after sealing; softens the food, so it feeds less.
 */
public record Canned(ItemStack food, float sterile, boolean sealed, float mush) {

    public static final Codec<Canned> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.CODEC.fieldOf("food").forGetter(Canned::food),
            Codec.FLOAT.optionalFieldOf("sterile", 0f).forGetter(Canned::sterile),
            Codec.BOOL.optionalFieldOf("sealed", false).forGetter(Canned::sealed),
            Codec.FLOAT.optionalFieldOf("mush", 0f).forGetter(Canned::mush)
    ).apply(instance, Canned::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Canned> STREAM_CODEC = StreamCodec.composite(
            ItemStack.STREAM_CODEC, Canned::food,
            ByteBufCodecs.FLOAT, Canned::sterile,
            ByteBufCodecs.BOOL, Canned::sealed,
            ByteBufCodecs.FLOAT, Canned::mush,
            Canned::new);
}
