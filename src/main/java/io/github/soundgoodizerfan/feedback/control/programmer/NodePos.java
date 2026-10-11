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
package io.github.soundgoodizerfan.feedback.control.programmer;

import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Where one card sits on a {@link ProgrammerScreen} canvas, in canvas pixels. The canvas is unbounded, so either may be negative. */
record NodePos(int x, int y) {

    static final StreamCodec<ByteBuf, NodePos> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, NodePos::x,
            ByteBufCodecs.VAR_INT, NodePos::y,
            NodePos::new);

    /** Keyed by node id -- a Programmer's whole layout, as committed by {@link ProgrammerCommitPayload}. */
    static final StreamCodec<ByteBuf, Map<Integer, NodePos>> MAP_STREAM_CODEC =
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.VAR_INT, STREAM_CODEC);
}
