// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.control.programmer;

import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Where one card sits on a {@code ProgrammerScreen} canvas, in canvas pixels. The canvas is unbounded, so either may be negative. */
public record NodePos(int x, int y) {

    static final StreamCodec<ByteBuf, NodePos> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, NodePos::x,
            ByteBufCodecs.VAR_INT, NodePos::y,
            NodePos::new);

    /** Keyed by node id -- a Programmer's whole layout, as committed by {@link ProgrammerCommitPayload}. */
    static final StreamCodec<ByteBuf, Map<Integer, NodePos>> MAP_STREAM_CODEC =
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, STREAM_CODEC);
}
