// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.item;

import java.util.Arrays;
import java.util.Base64;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** The picture a photograph carries: {@code WIDTH} by {@code HEIGHT} pixels of three bytes (red, green, blue), row by row. */
public record Photo(byte[] rgb) {

    public static final int WIDTH = 64, HEIGHT = 48;

    public static final Codec<Photo> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.xmap(s -> Base64.getDecoder().decode(s), b -> Base64.getEncoder().encodeToString(b)).fieldOf("rgb").forGetter(Photo::rgb)
    ).apply(i, Photo::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, Photo> STREAM_CODEC =
            ByteBufCodecs.byteArray(WIDTH * HEIGHT * 3).map(Photo::new, Photo::rgb).cast();

    public boolean valid() {
        return rgb.length == WIDTH * HEIGHT * 3;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Photo p && Arrays.equals(rgb, p.rgb);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(rgb);
    }
}
