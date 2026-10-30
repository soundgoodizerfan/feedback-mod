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
package io.github.soundgoodizerfan.feedback.net;

import java.util.ArrayList;
import java.util.List;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.potion.PotionBrew;
import io.github.soundgoodizerfan.feedback.potion.PotionShape;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * The server tells the client how the brewing stand is: where the marker is, the path ahead, and the marks this
 * player may see. Hazards and vortices always; effect nodes only once this player has made them; a residue mark only as a direction.
 *
 * @param path   the curves ahead, in order: each ingredient not yet stirred (or part way) as its start, segment and how much of it is walked.
 * @param marks  what is on the map.
 * @param fog    the cells this player has uncovered, a bit each.
 */
public record StandStatePayload(BlockPos pos, float x, float y, float potency, float temperature, int water, int ingredients,
                                int stirred, float rotDegrees, float boundary, String last, List<Seg> path, List<Mark> marks, long[] fog) implements CustomPacketPayload {

    /** One segment of the path ahead: where it starts, its curve, and the arc fraction already walked (0 if unstarted). */
    public record Seg(float x, float y, PotionBrew.Segment segment, float from) {

        /** The point after walking arc fraction {@code s}, in map units, written into {@code out}. */
        public void at(float s, float[] out) {
            segment.at(s, out);
            out[0] += x;
            out[1] += y;
        }
    }

    /** A thing on the map. kind: 0 precipitate zone, 1 scorch zone (a is the temperature it starts at), 2 vortex (a, b the landing, NaN until used), 3 a node made (label is its effect), 4 a residue (label is the compound class the nearest unmade node leans on), 6 the faint line from the origin to a node at (x, y), ending in its icon (a is 1 if the node is made, else a bare silhouette), 7 a landmark. */
    public record Mark(int kind, float x, float y, float radius, float a, float b, String label) {}

    public static final Type<StandStatePayload> TYPE = new Type<>(Feedback.id("stand_state"));

    // Twelve fields are past StreamCodec.composite's six.
    public static final StreamCodec<RegistryFriendlyByteBuf, StandStatePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeBlockPos(p.pos);
                buf.writeFloat(p.x);
                buf.writeFloat(p.y);
                buf.writeFloat(p.potency);
                buf.writeFloat(p.temperature);
                buf.writeVarInt(p.water);
                buf.writeVarInt(p.ingredients);
                buf.writeVarInt(p.stirred);
                buf.writeFloat(p.rotDegrees);
                buf.writeFloat(p.boundary);
                buf.writeVarInt(p.fog.length);
                for (long bits : p.fog)
                    buf.writeLong(bits);
                buf.writeUtf(p.last, 64);
                buf.writeVarInt(p.path.size());
                for (Seg q : p.path) {
                    PotionBrew.Segment g = q.segment;
                    buf.writeFloat(q.x);
                    buf.writeFloat(q.y);
                    buf.writeFloat(q.from);
                    buf.writeFloat(g.heading());
                    buf.writeFloat(g.reach());
                    buf.writeFloat(g.dx());
                    buf.writeFloat(g.dy());
                    buf.writeByte(g.shape().type().ordinal());
                    buf.writeFloat(g.shape().p1());
                    buf.writeFloat(g.shape().p2());
                }
                buf.writeVarInt(p.marks.size());
                for (Mark m : p.marks) {
                    buf.writeByte(m.kind);
                    buf.writeFloat(m.x);
                    buf.writeFloat(m.y);
                    buf.writeFloat(m.radius);
                    buf.writeFloat(m.a);
                    buf.writeFloat(m.b);
                    buf.writeUtf(m.label, 64);
                }
            },
            buf -> {
                BlockPos pos = buf.readBlockPos();
                float x = buf.readFloat(), y = buf.readFloat(), potency = buf.readFloat(), temperature = buf.readFloat();
                int water = buf.readVarInt(), ingredients = buf.readVarInt(), stirred = buf.readVarInt();
                float rot = buf.readFloat(), boundary = buf.readFloat();
                long[] fog = new long[Math.min(buf.readVarInt(), 64)];
                for (int i = 0; i < fog.length; i++)
                    fog[i] = buf.readLong();
                String last = buf.readUtf(64);
                int n = Math.min(buf.readVarInt(), 16);
                List<Seg> path = new ArrayList<>(n);
                for (int i = 0; i < n; i++) {
                    float sx = buf.readFloat(), sy = buf.readFloat(), from = buf.readFloat(), heading = buf.readFloat(), reach = buf.readFloat(), dx = buf.readFloat(), dy = buf.readFloat();
                    PotionShape.Type type = PotionShape.Type.values()[Math.floorMod(buf.readByte(), PotionShape.Type.values().length)];
                    path.add(new Seg(sx, sy, new PotionBrew.Segment(heading, reach, new PotionShape(type, buf.readFloat(), buf.readFloat()), dx, dy), from));
                }
                int m = Math.min(buf.readVarInt(), 128);
                List<Mark> marks = new ArrayList<>(m);
                for (int i = 0; i < m; i++)
                    marks.add(new Mark(buf.readByte(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readUtf(64)));
                return new StandStatePayload(pos, x, y, potency, temperature, water, ingredients, stirred, rot, boundary, last, path, marks, fog);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StandStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> io.github.soundgoodizerfan.feedback.client.StandScreen.state(payload));
    }
}
