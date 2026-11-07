// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.frame;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Select mode's first corner, in the frame's 1/16 cells: the second click closes the box. */
public record WelderCorner(BlockPos frame, int x, int y, int z) {

    public static final Codec<WelderCorner> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("frame").forGetter(WelderCorner::frame),
            Codec.INT.fieldOf("x").forGetter(WelderCorner::x),
            Codec.INT.fieldOf("y").forGetter(WelderCorner::y),
            Codec.INT.fieldOf("z").forGetter(WelderCorner::z)
    ).apply(i, WelderCorner::new));

    public static final StreamCodec<ByteBuf, WelderCorner> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WelderCorner::frame,
            ByteBufCodecs.VAR_INT, WelderCorner::x,
            ByteBufCodecs.VAR_INT, WelderCorner::y,
            ByteBufCodecs.VAR_INT, WelderCorner::z,
            WelderCorner::new);
}
