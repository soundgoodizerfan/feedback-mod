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
