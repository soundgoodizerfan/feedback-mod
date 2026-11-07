// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.frame;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.List;

/** The points chosen so far for a plate, in {@link WelderMode#PLATE}. Same reasoning as {@link WelderSelection}. */
public record WelderPicks(BlockPos frame, List<Integer> vertices) {

    public static final Codec<WelderPicks> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("frame").forGetter(WelderPicks::frame),
            Codec.INT.listOf().fieldOf("vertices").forGetter(WelderPicks::vertices)
    ).apply(i, WelderPicks::new));

    public static final StreamCodec<ByteBuf, WelderPicks> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WelderPicks::frame,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), WelderPicks::vertices,
            WelderPicks::new);
}
