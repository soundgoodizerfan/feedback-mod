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
 * Assets under src/main/resources are NOT covered by this licence.
 * See LICENSE-ASSETS.
 */
package io.github.soundgoodizerfan.feedback.combat.tether;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * One line holding a living thing: the abstract held state. A harpoon barb is the first source; grapple, grab, net and snare are the others. The tether
 * is data only -- the source runs the rope and the reel, {@link Tethers} keeps the list and answers
 * how much a held body suffers.
 *
 * @param sourceId      the thing that made it (the harpoon entity's uuid); one tether per source
 * @param anchor        what the line is fixed to; the held body is reeled toward it
 * @param ropeLength    the line's length in blocks
 * @param reelRate      blocks per tick the line draws the body in; 0 for none
 * @param vulnerability damage-taken multiplier while held
 * @param release       when the source lets go on its own
 */
public record Tether(UUID sourceId, Anchor anchor, float ropeLength, float reelRate, float vulnerability, Release release) {

    /** What the line is fixed to. */
    public sealed interface Anchor {
        record OnEntity(int entityId) implements Anchor {
        }

        record OnBlock(BlockPos pos) implements Anchor {
        }

        record AtPoint(Vec3 point) implements Anchor {
        }

        StreamCodec<ByteBuf, Anchor> STREAM_CODEC = StreamCodec.of(
                (buf, a) -> {
                    switch (a) {
                        case OnEntity e -> {
                            buf.writeByte(0);
                            ByteBufCodecs.VAR_INT.encode(buf, e.entityId());
                        }
                        case OnBlock b -> {
                            buf.writeByte(1);
                            BlockPos.STREAM_CODEC.encode(buf, b.pos());
                        }
                        case AtPoint p -> {
                            buf.writeByte(2);
                            buf.writeDouble(p.point().x).writeDouble(p.point().y).writeDouble(p.point().z);
                        }
                    }
                },
                buf -> switch (buf.readByte()) {
                    case 0 -> new OnEntity(ByteBufCodecs.VAR_INT.decode(buf));
                    case 1 -> new OnBlock(BlockPos.STREAM_CODEC.decode(buf));
                    default -> new AtPoint(new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()));
                });
    }

    /**
     * The source lets go when the body is within {@code stopDistance} of the anchor or the reel has
     * run {@code maxTicks}.
     */
    public record Release(double stopDistance, int maxTicks) {
        public static final StreamCodec<ByteBuf, Release> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, Release::stopDistance,
                ByteBufCodecs.VAR_INT, Release::maxTicks,
                Release::new);
    }

    public static final StreamCodec<ByteBuf, Tether> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, Tether::sourceId,
            Anchor.STREAM_CODEC, Tether::anchor,
            ByteBufCodecs.FLOAT, Tether::ropeLength,
            ByteBufCodecs.FLOAT, Tether::reelRate,
            ByteBufCodecs.FLOAT, Tether::vulnerability,
            Release.STREAM_CODEC, Tether::release,
            Tether::new);
}
