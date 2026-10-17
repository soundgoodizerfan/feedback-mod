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
package io.github.soundgoodizerfan.feedback.vehicle.frame;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;

/**
 * The points a Welder has selected on one frame, in the order they were taken: what the gizmo
 * moves, Extrude pulls out, Remove takes away, and -- when every one was clicked by hand rather
 * than boxed ({@code clicked}) -- what the Pivot Setter sets together (author, 2026-10-16).
 */
public record WelderSelected(BlockPos frame, List<Integer> vertices, boolean clicked) {

    public static final Codec<WelderSelected> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.fieldOf("frame").forGetter(WelderSelected::frame),
            Codec.INT.listOf().fieldOf("vertices").forGetter(WelderSelected::vertices),
            Codec.BOOL.optionalFieldOf("clicked", true).forGetter(WelderSelected::clicked)
    ).apply(i, WelderSelected::new));

    public static final StreamCodec<ByteBuf, WelderSelected> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, WelderSelected::frame,
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), WelderSelected::vertices,
            ByteBufCodecs.BOOL, WelderSelected::clicked,
            WelderSelected::new);

    /** With {@code v} added, or taken out if it was in. */
    public WelderSelected toggled(int v) {
        List<Integer> next = new ArrayList<>(vertices);
        if (!next.remove((Integer) v))
            next.add(v);
        return new WelderSelected(frame, next, clicked);
    }

    /** The point taken last, or -1. */
    public int last() {
        return vertices.isEmpty() ? -1 : vertices.get(vertices.size() - 1);
    }
}
