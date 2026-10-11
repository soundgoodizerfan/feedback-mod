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
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
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
 * Turns a welded {@link Frame} into Sable bodies ({@code feedback_vehicles_spec.md} section 4,
 * <em>Bridging to Sable</em>). A body is a frame block alone -- the one block Sable needs to have a
 * body at all -- and the plates and tubes are {@link FrameBodies free boxes} welded to it, so
 * nothing is placed in the world but frame blocks.
 *
 * <h2>Pivots cut the frame</h2>
 * A vertex set to be a bearing ({@link Frame#pivots()}) is where a frame stops being one body. On
 * assembly the frame is cut there: everything reachable from a side without crossing a pivot is
 * one part, each part gets a frame block of its own (the first keeps the original, the rest take
 * free air nearby with their vertices shifted so nothing moves), and each pivot becomes a
 * {@link FrameJoint} between the parts it separated -- a copy of the vertex sits in each, at the
 * same place. A pivot is therefore only a setting until the chassis is a physics object; there it
 * activates and works as any joint between frames does.
 */
public final class FrameAssembly {

    public enum Result { ASSEMBLED, NOTHING_TO_ASSEMBLE, BLOCKED }

    /** How far from the frame block a part's new frame block may be put, in blocks. */
    private static final int ROOM = 4;

    private FrameAssembly() {
    }

    /** One side of a cut: the vertices, edges and plates it keeps. */
    record Part(Set<Integer> vertices, Set<Long> edges, List<Frame.Plate> plates, int solids) {
    }

    /**
     * Lifts the frame block into a Sable body and marks the frame assembled; its boxes appear on
     * the frame's next tick. Refuses if there is no plate or tube to make a box of. A frame with
     * pivots becomes one body per part (see the class doc) and the first part's body is returned.
     */
    @Nullable
    public static ServerSubLevel assemble(ServerLevel level, BlockPos framePos, FrameBlockEntity be, Result[] result) {
        Frame frame = be.frame();
        if (frame.plates().isEmpty() && frame.tubes().isEmpty()) {
            result[0] = Result.NOTHING_TO_ASSEMBLE;
            return null;
        }
        List<Part> parts = parts(frame);
        if (parts.size() > 1) {
            List<BlockPos> spots = freeSpots(level, framePos, parts.size() - 1);
            if (spots == null) {
                result[0] = Result.BLOCKED;
                return null;
            }
            cut(level, framePos, be, parts, spots);
        }
        result[0] = Result.ASSEMBLED;
        return lift(level, framePos, be);
    }

    private static ServerSubLevel lift(ServerLevel level, BlockPos pos, FrameBlockEntity be) {
        be.setAssembled(true);
        return SubLevelAssemblyHelper.assembleBlocks(level, pos, List.of(pos),
                new BoundingBox3i(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ()));
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
                out.add(new Part(verts.get(key), edges.get(key), plates.get(key), solids));
        }
        out.sort((x, y) -> Integer.compare(y.solids(), x.solids()));
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

    /** {@code count} distinct air blocks near {@code around}, nearest first; null if there are not that many. */
    @Nullable
    private static List<BlockPos> freeSpots(ServerLevel level, BlockPos around, int count) {
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(around.offset(-ROOM, -ROOM, -ROOM), around.offset(ROOM, ROOM, ROOM)))
            if (!p.equals(around) && level.getBlockState(p).isAir())
                found.add(p.immutable());
        if (found.size() < count)
            return null;
        found.sort((x, y) -> Double.compare(x.distSqr(around), y.distSqr(around)));
        return found.subList(0, count);
    }

    /**
     * Gives each part after the first a frame block of its own, trims {@code be} to the first part,
     * and makes each pivot a joint between the parts it touches. Every part is lifted into its own
     * body. The pivot's vertex exists in each part it touches, so a joint's two ends name it the
     * same.
     */
    private static void cut(ServerLevel level, BlockPos framePos, FrameBlockEntity be, List<Part> parts, List<BlockPos> spots) {
        Frame whole = be.frame();
        Map<Integer, Frame.Pivot> pivots = new HashMap<>(whole.pivots());
        FrameBlockEntity[] owners = new FrameBlockEntity[parts.size()];
        owners[0] = be;
        for (int i = 1; i < parts.size(); i++) {
            BlockPos at = spots.get(i - 1);
            level.setBlock(at, FVehicles.FRAME.get().defaultBlockState(), 3);
            owners[i] = (FrameBlockEntity) level.getBlockEntity(at);
            Part part = parts.get(i);
            // The new block sits at a different place, so vertices are re-based to land where they were.
            int dx = (framePos.getX() - at.getX()) * Frame.GRID, dy = (framePos.getY() - at.getY()) * Frame.GRID,
                    dz = (framePos.getZ() - at.getZ()) * Frame.GRID;
            owners[i].frame().load(whole.part(part.vertices(), part.edges(), part.plates(), dx, dy, dz).save());
        }
        Part first = parts.get(0);
        whole.load(whole.part(first.vertices(), first.edges(), first.plates(), 0, 0, 0).save());

        for (Map.Entry<Integer, Frame.Pivot> e : pivots.entrySet()) {
            int v = e.getKey();
            FrameBlockEntity anchor = null;
            for (int i = 0; i < parts.size(); i++) {
                if (!parts.get(i).vertices().contains(v))
                    continue;
                if (anchor == null)
                    anchor = owners[i];
                else
                    anchor.join(v, owners[i], v, e.getValue().kind(), e.getValue().axis(), e.getValue().drives());
            }
        }
        for (FrameBlockEntity owner : owners)
            owner.changed();
        for (int i = 1; i < owners.length; i++)
            lift(level, owners[i].getBlockPos(), owners[i]);
    }
}
