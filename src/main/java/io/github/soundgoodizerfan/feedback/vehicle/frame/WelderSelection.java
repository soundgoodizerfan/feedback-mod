// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.vehicle.frame;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Which vertex a Welder has hold of, and whether it is being carried. Lives on the item stack
 * rather than in a server-side map so the client gets it for free and can draw it.
 */
public record WelderSelection(BlockPos frame, int vertex, boolean grabbed) {

    public static final Codec<WelderSelection> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("frame").forGetter(WelderSelection::frame),
            Codec.INT.fieldOf("vertex").forGetter(WelderSelection::vertex),
            Codec.BOOL.fieldOf("grabbed").forGetter(WelderSelection::grabbed)
    ).apply(i, WelderSelection::new));

    public static final StreamCodec<ByteBuf, WelderSelection> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WelderSelection::frame,
            ByteBufCodecs.VAR_INT, WelderSelection::vertex,
            ByteBufCodecs.BOOL, WelderSelection::grabbed,
            WelderSelection::new);
}
