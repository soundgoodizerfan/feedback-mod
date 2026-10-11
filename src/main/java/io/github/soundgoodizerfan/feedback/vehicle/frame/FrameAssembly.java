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

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Turns a welded {@link Frame} into a Sable body ({@code feedback_vehicles_spec.md} section 4,
 * <em>Bridging to Sable</em>). The body is the frame block alone -- the one block Sable needs to
 * have a body at all -- and the plates and tubes are {@link FrameBodies free boxes} welded to it,
 * so nothing is placed in the world and nothing but the frame block is lifted out of it.
 *
 * <p>A chassis is always one frame block, however many bearings it has. A pivot cuts the frame
 * into {@link #parts parts} only in how {@link FrameBodies} holds its boxes: one part is welded
 * to the body, every other part is a rigid cluster of boxes that hangs off the body by the
 * bearing, with no block of its own.
 */
public final class FrameAssembly {

    public enum Result { ASSEMBLED, NOTHING_TO_ASSEMBLE, BLOCKED }

    private FrameAssembly() {
    }

    /**
     * One side of a cut: the vertices, edges and plates it keeps, and how many plates and tubes
     * that makes. {@code id} is its lowest vertex id, which stays put as the frame is edited.
     */
    record Part(int id, Set<Integer> vertices, Set<Long> edges, List<Frame.Plate> plates, int solids) {
    }

    /**
     * Lifts the frame block into a Sable body and marks the frame assembled; its boxes appear on
     * the frame's next tick. Refuses if there is no plate or tube to make a box of.
     */
    @Nullable
    public static ServerSubLevel assemble(ServerLevel level, BlockPos framePos, FrameBlockEntity be, Result[] result) {
        Frame frame = be.frame();
        if (frame.plates().isEmpty() && frame.tubes().isEmpty()) {
            result[0] = Result.NOTHING_TO_ASSEMBLE;
            return null;
        }
        be.setAssembled(true);
        result[0] = Result.ASSEMBLED;
        return SubLevelAssemblyHelper.assembleBlocks(level, framePos, List.of(framePos),
                new BoundingBox3i(framePos.getX(), framePos.getY(), framePos.getZ(),
                        framePos.getX(), framePos.getY(), framePos.getZ()));
    }

    /**
     * The parts of {@code frame} once cut at its pivots, largest first, keeping only parts with a
     * plate or tube -- bare construction lines are not a body. One part means nothing to cut.
     *
     * <p>Two non-pivot vertices on one edge are one side, so they are united; a plate or tube
     * belongs to the side of its non-pivot vertex. An edge between two pivots, with nothing on it
     * to join it to a side (a link rod between two bearings), is a part of its own.
     */
    static List<Part> parts(Frame frame) {
        Set<Integer> pivots = frame.pivots().keySet();
        if (pivots.isEmpty())
            return List.of();
        Map<Integer, Integer> root = new HashMap<>();
        for (int v : frame.vertices().keySet())
            if (!pivots.contains(v))
                root.put(v, v);
        for (long key : frame.edgeKeys()) {
            int a = Frame.edgeA(key), b = Frame.edgeB(key);
            if (root.containsKey(a) && root.containsKey(b))
                root.put(find(root, a), find(root, b));
        }
        Map<Object, Set<Integer>> verts = new LinkedHashMap<>();
        Map<Object, Set<Long>> edges = new LinkedHashMap<>();
        Map<Object, List<Frame.Plate>> plates = new LinkedHashMap<>();
        for (int v : root.keySet())
            side(verts, edges, plates, find(root, v)).add(v);

        for (Frame.Plate p : frame.plates()) {
            int[] ids = {p.a(), p.b(), p.c()};
            Object key = null;
            for (int v : ids)
                if (root.containsKey(v)) {
                    key = find(root, v);
                    break;
                }
            if (key == null)
                key = Frame.edgeKey(p.a(), p.b()); // three pivots: a part of its own
            side(verts, edges, plates, key);
            plates.get(key).add(p);
            for (int i = 0; i < 3; i++) {
                verts.get(key).add(ids[i]);
                edges.get(key).add(Frame.edgeKey(ids[i], ids[(i + 1) % 3]));
            }
        }
        for (long key : frame.edgeKeys()) {
            int a = Frame.edgeA(key), b = Frame.edgeB(key);
            Object side = root.containsKey(a) ? find(root, a) : root.containsKey(b) ? find(root, b) : (Object) key;
            side(verts, edges, plates, side);
            edges.get(side).add(key);
            verts.get(side).add(a);
            verts.get(side).add(b);
        }

        List<Part> out = new ArrayList<>();
        for (Object key : verts.keySet()) {
            int solids = plates.get(key).size();
            for (long e : edges.get(key))
                if (frame.tubes().containsKey(e))
                    solids++;
            if (solids > 0)
                out.add(new Part(verts.get(key).stream().mapToInt(Integer::intValue).min().orElse(0),
                        verts.get(key), edges.get(key), plates.get(key), solids));
        }
        out.sort((x, y) -> x.solids() != y.solids() ? Integer.compare(y.solids(), x.solids()) : Integer.compare(x.id(), y.id()));
        return out;
    }

    private static Set<Integer> side(Map<Object, Set<Integer>> verts, Map<Object, Set<Long>> edges,
                                     Map<Object, List<Frame.Plate>> plates, Object key) {
        edges.computeIfAbsent(key, k -> new HashSet<>());
        plates.computeIfAbsent(key, k -> new ArrayList<>());
        return verts.computeIfAbsent(key, k -> new HashSet<>());
    }

    private static int find(Map<Integer, Integer> root, int v) {
        int r = v;
        while (root.get(r) != r)
            r = root.get(r);
        root.put(v, r);
        return r;
    }
}
