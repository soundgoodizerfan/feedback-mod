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
package io.github.soundgoodizerfan.feedback.potion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * A tool's soak in a potion (potions spec 12.5): a short rider on each blow, spent by blows and never by waiting.
 *
 * @param effect    the node's effect id (a mob effect or a {@link PotionEffects} special).
 * @param amplifier the rider's level (0 is I).
 * @param duration  ticks of the rider on a struck body.
 * @param hits      blows left; at 0 the component is removed.
 */
public record Imbue(ResourceLocation effect, int amplifier, int duration, int hits) {

    public static final Codec<Imbue> CODEC = RecordCodecBuilder.create(i -> i.group(
            ResourceLocation.CODEC.fieldOf("effect").forGetter(Imbue::effect),
            Codec.INT.fieldOf("amplifier").forGetter(Imbue::amplifier),
            Codec.INT.fieldOf("duration").forGetter(Imbue::duration),
            Codec.INT.fieldOf("hits").forGetter(Imbue::hits)
    ).apply(i, Imbue::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Imbue> STREAM_CODEC = ByteBufCodecs.fromCodecWithRegistries(CODEC);
}
