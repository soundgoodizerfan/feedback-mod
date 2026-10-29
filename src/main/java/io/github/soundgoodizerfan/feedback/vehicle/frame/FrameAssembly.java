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
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import io.github.soundgoodizerfan.feedback.core.unit.Mu;
import io.github.soundgoodizerfan.feedback.core.unit.Units;
import io.github.soundgoodizerfan.feedback.process.Material;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Turns a welded {@link Frame} into blocks Sable can lift ({@code docs/feedback_vehicles_spec.md}
 * section 4, <em>cells, again</em>). The frame is cut at its bearings into {@link #parts parts};
 * each part is traced into {@link ChassisCellBlock cells}; a part welded to the base by one of its
 * own points goes up with the base, and every other part is lifted on its own, a body of its own,
 * and hangs by its bearings ({@link FrameBlockEntity} makes those joints).
 *
 * <p>What a cell is: the frame through one block, as the octants it crosses (physics) and as its
 * 1/16 voxels (players and the outline). A cell is placed only where the world is air -- the
 * blocks of the base already stand where they stand -- and never inside a tire's reach of its hub:
 * the tire is a ray, and a half-block octant round the axle would drag on the road below it.
 */
public final class FrameAssembly {

    /** Sample spacing across a plate, in blocks: finer than a voxel, so none a plate crosses is missed. */
    private static final double SAMPLE = 1.0 / 32;
    private static final int G = Frame.GRID;
    /** How far round a cutting bearing both sides keep no physics cell, in blocks. */
    private static final double BEARING_CLEAR = 0.5;

    public enum Result { ASSEMBLED, NOTHING_TO_ASSEMBLE }

    private FrameAssembly() {
    }

    /**
     * One side of a cut: the vertices, edges and plates it keeps, and how many plates and tubes
     * that makes. {@code id} is its lowest non-pivot vertex id, which stays put as the frame is
     * edited and is never shared with another side.
     */
    public record Part(int id, Set<Integer> vertices, Set<Long> edges, List<Frame.Plate> plates, int solids) {
    }

    /** One block of a part as traced: its octants, its voxels, and the mass it carries, kpg. */
    public static final class Cell {
        int octants;
        final BitSet voxels = new BitSet(G * G * G);
        double mass;

        void add(Cell o) {
            octants |= o.octants;
            voxels.or(o.voxels);
            mass += o.mass;
        }

        public int octants() {
            return octants;
        }

        public double mass() {
            return mass;
        }
    }

    /**
     * The groups assembly makes bodies of: the {@link #parts parts}, or the whole frame as group 0
     * when no bearing cuts it.
     */
    public static List<Part> groups(Frame frame) {
        List<Part> cut = parts(frame);
        if (!cut.isEmpty())
            return cut;
        if (frame.plates().isEmpty() && frame.tubes().isEmpty())
            return List.of();
        return List.of(new Part(0, new HashSet<>(frame.vertices().keySet()), new HashSet<>(frame.tubes().keySet()),
                new ArrayList<>(frame.plates()), frame.plates().size() + frame.tubes().size()));
    }

    /**
     * True if a point of {@code part} that is not a bearing is welded to a block that is there
     * ({@code present}, by offset from the frame block): the part is rigid with the base. A weld
     * on a bearing that cuts is that bearing, so it does not count; one on a bearing that does not
     * cut (locked, or a driving hinge) holds like any weld.
     */
    public static boolean welded(Frame frame, Part part, Predicate<BlockPos> present) {
        for (Map.Entry<Integer, BlockPos> e : frame.anchors().entrySet())
            if (part.vertices().contains(e.getKey()) && !cutsAt(frame, e.getKey()) && present.test(e.getValue()))
                return true;
        return false;
    }

    /**
     * Places the cells of every group of the frame at {@code framePos}, lifts each group that is
     * not welded into a body of its own, and returns where the welded groups' cells went, for the
     * caller to lift with the base. Marks the frame assembled.
     */
    public static List<BlockPos> cut(ServerLevel level, BlockPos framePos, FrameBlockEntity be) {
        Frame frame = be.frame();
        Predicate<BlockPos> present = off -> !level.getBlockState(framePos.offset(off)).isAir();
        be.clearCells();
        int[] blocked = new int[1];
        Map<BlockPos, Cell> rigid = new HashMap<>();
        for (Part g : groups(frame)) {
            Map<BlockPos, Cell> cells = cells(frame, g);
            if (welded(frame, g, present)) {
                cells.forEach((rel, c) -> rigid.computeIfAbsent(rel, r -> new Cell()).add(c));
                continue;
            }
            Set<BlockPos> rel = place(level, framePos, cells, true, be.id(), blocked);
            if (rel.isEmpty())
                continue;
            List<BlockPos> blocks = rel.stream().map(framePos::offset).toList();
            BlockPos anchor = blocks.get(0);
            ServerSubLevel body = SubLevelAssemblyHelper.assembleBlocks(level, anchor, blocks, BoundingBox3i.from(blocks));
            be.addGroupBody(g.id(), body.getUniqueId(),
                    framePos.subtract(anchor).offset(body.getPlot().getCenterBlock()), rel);
        }
        Set<BlockPos> rel = place(level, framePos, rigid, true, be.id(), blocked);
        be.setBaseCells(rel);
        be.setAssembled(true);
        be.setBlockedCells(blocked[0]);
        return rel.stream().map(framePos::offset).toList();
    }

    /**
     * Lifts the frame block and its welded cells, and nothing else, into a Sable body; every other
     * group becomes a body of its own. Refuses if there is no plate or tube. The Lift is the way in
     * play; this is the frame alone, for tests.
     */
    @Nullable
    public static ServerSubLevel assemble(ServerLevel level, BlockPos framePos, FrameBlockEntity be, Result[] result) {
        if (be.frame().plates().isEmpty() && be.frame().tubes().isEmpty()) {
            result[0] = Result.NOTHING_TO_ASSEMBLE;
            return null;
        }
        List<BlockPos> blocks = new ArrayList<>();
        blocks.add(framePos);
        blocks.addAll(cut(level, framePos, be));
        result[0] = Result.ASSEMBLED;
        return SubLevelAssemblyHelper.assembleBlocks(level, framePos, blocks, BoundingBox3i.from(blocks));
    }

    /**
     * Brings an assembled frame's cells in line with an edit. Cells go where their group already
     * is -- a group with a body of its own re-cut in that body's plot, every other group in the
     * frame's -- and a group whose body no longer matches any group is taken out of the world. A
     * group the edit newly cut off stays rigid with the frame until the vehicle is lifted again:
     * moving cells between bodies is re-assembly, and that is the Lift's job. A new cell that would
     * land on another block is skipped, and the nearby players are told how many.
     */
    static void recut(ServerLevel level, FrameBlockEntity be) {
        Frame frame = be.frame();
        BlockPos framePos = be.getBlockPos();
        Map<Integer, FrameBlockEntity.GroupBody> bodies = be.groupBodies();
        List<Part> groups = groups(frame);
        Set<Integer> ids = new HashSet<>();
        groups.forEach(g -> ids.add(g.id()));
        for (Integer id : List.copyOf(bodies.keySet()))
            if (!ids.contains(id)) {
                destroy(level, bodies.get(id));
                be.removeGroupBody(id);
            }
        Predicate<BlockPos> present = off -> !level.getBlockState(framePos.offset(off)).isAir();
        int[] blocked = new int[1];
        Map<BlockPos, Cell> rigid = new HashMap<>();
        for (Part g : groups) {
            Map<BlockPos, Cell> cells = cells(frame, g);
            FrameBlockEntity.GroupBody body = bodies.get(g.id());
            if (body == null || welded(frame, g, present)) {
                cells.forEach((rel, c) -> rigid.computeIfAbsent(rel, r -> new Cell()).add(c));
                continue;
            }
            Set<BlockPos> rel = replace(level, body.origin(), body.cells(), cells, be.id(), blocked);
            body.cells().clear();
            body.cells().addAll(rel);
        }
        be.setBaseCells(replace(level, framePos, be.baseCells(), rigid, be.id(), blocked));
        be.setBlockedCells(blocked[0]);
        if (blocked[0] > 0)
            for (var player : level.players())
                if (player.blockPosition().closerThan(framePos, 24))
                    player.displayClientMessage(Component.translatable("message.feedback.frame.cells_blocked", blocked[0]), true);
    }

    /** Swaps the cells at {@code origin + old} for {@code want}; returns those placed. */
    private static Set<BlockPos> replace(ServerLevel level, BlockPos origin, Set<BlockPos> old, Map<BlockPos, Cell> want, java.util.UUID frame, int[] blocked) {
        for (BlockPos rel : old)
            if (!want.containsKey(rel) && level.getBlockState(origin.offset(rel)).is(FVehicles.CHASSIS_CELL.get()))
                level.setBlock(origin.offset(rel), Blocks.AIR.defaultBlockState(), 3);
        return place(level, origin, want, false, frame, blocked);
    }

    /** Takes a group's body out of the world, cells and all. */
    static void destroy(ServerLevel level, FrameBlockEntity.GroupBody group) {
        var container = ServerSubLevelContainer.getContainer(level);
        if (container != null && container.getSubLevel(group.body()) instanceof ServerSubLevel s && !s.isRemoved()) {
            s.getPlot().destroyAllBlocks();
            container.removeSubLevel(s, SubLevelRemovalReason.REMOVED);
        }
    }

    /**
     * Puts each cell at {@code origin + rel} where the world is air (or, when {@code fresh} is
     * false, already a cell), with its voxels. Returns those placed.
     */
    private static Set<BlockPos> place(ServerLevel level, BlockPos origin, Map<BlockPos, Cell> cells, boolean fresh, java.util.UUID frame, int[] blocked) {
        // Where a cell cannot go (a block of the base stands there, or a tire's reach emptied it)
        // its mass is shared out over the cells that can, so the vehicle still weighs its plates.
        double total = 0, kept = 0;
        Map<BlockPos, Cell> placeable = new HashMap<>();
        for (Map.Entry<BlockPos, Cell> e : cells.entrySet()) {
            Cell c = e.getValue();
            total += c.mass;
            BlockState there = level.getBlockState(origin.offset(e.getKey()));
            if (c.octants == 0)
                continue;
            if (!there.isAir() && (fresh || !there.is(FVehicles.CHASSIS_CELL.get()))) {
                blocked[0]++;
                continue;
            }
            placeable.put(e.getKey(), c);
            kept += c.mass;
        }
        double scale = kept > 0 ? total / kept : 1;
        Set<BlockPos> out = new HashSet<>();
        placeable.forEach((rel, c) -> {
            BlockPos at = origin.offset(rel);
            BlockState there = level.getBlockState(at);
            BlockState state = FVehicles.CHASSIS_CELL.get().defaultBlockState()
                    .setValue(ChassisCellBlock.OCTANTS, c.octants)
                    .setValue(ChassisCellBlock.LOAD, ChassisCellBlock.loadFor(c.mass * scale));
            if (!there.equals(state))
                level.setBlock(at, state, 3);
            if (level.getBlockEntity(at) instanceof ChassisCellBlockEntity cell) {
                cell.setVoxels(c.voxels);
                cell.setFrame(frame);
            }
            out.add(rel);
        });
        return out;
    }

    /**
     * The cells of one group, by offset from the frame block. A plate is sampled across its plane
     * and a tube along its line, each sample marking its 1/16 voxel (a tube: the square of its
     * diameter round it) and the half-block octant that voxel is in; mass is spread over the
     * samples. Last, every octant within a tire's radius of a hub is cleared.
     */
    public static Map<BlockPos, Cell> cells(Frame frame, Part group) {
        Map<BlockPos, Cell> out = new HashMap<>();
        for (Frame.Plate p : group.plates()) {
            List<Vec3> samples = samples(frame.pos(p.a()), frame.pos(p.b()), frame.pos(p.c()));
            double each = mass(frame.volume(p), p.materialState()) / samples.size();
            for (Vec3 s : samples)
                mark(out, (int) Math.floor(s.x * G), (int) Math.floor(s.y * G), (int) Math.floor(s.z * G), each);
        }
        for (long key : group.edges()) {
            Frame.Tube tube = frame.tubes().get(key);
            if (tube == null)
                continue;
            List<Vec3> samples = samples(frame.pos(Frame.edgeA(key)), frame.pos(Frame.edgeB(key)));
            double each = mass(frame.tubeVolume(key), tube.materialState()) / samples.size();
            double r = tube.diameter() * G / 2;
            for (Vec3 s : samples) {
                double cx = s.x * G, cy = s.y * G, cz = s.z * G;
                boolean first = true;
                for (int x = (int) Math.floor(cx - r + 0.5); x < cx + r; x++)
                    for (int y = (int) Math.floor(cy - r + 0.5); y < cy + r; y++)
                        for (int z = (int) Math.floor(cz - r + 0.5); z < cz + r; z++) {
                            mark(out, x, y, z, first ? each : 0);
                            first = false;
                        }
            }
        }
        frame.tires().forEach((vertex, tire) -> {
            if (frame.vertices().containsKey(vertex))
                clear(out, frame.pos(vertex), tire.radius());
        });
        // Round a bearing that cuts, both sides' cells would meet: the joint holds the two bodies
        // there and the colliders would fight it. Cleared, the two sides may collide everywhere
        // else -- an arm swings into the chassis and stops, rather than through it.
        frame.pivots().forEach((vertex, pivot) -> {
            if (pivot.cuts() && group.vertices().contains(vertex))
                clear(out, frame.pos(vertex), BEARING_CLEAR);
        });
        return out;
    }

    /** Empties every octant of {@code out} that comes nearer {@code centre} than {@code r}. */
    private static void clear(Map<BlockPos, Cell> out, Vec3 centre, double r) {
        out.forEach((pos, c) -> {
            for (int i = 0; i < 8; i++) {
                double x0 = pos.getX() + (i & 1) * 0.5, y0 = pos.getY() + (i >> 1 & 1) * 0.5, z0 = pos.getZ() + (i >> 2 & 1) * 0.5;
                double dx = centre.x - Math.clamp(centre.x, x0, x0 + 0.5), dy = centre.y - Math.clamp(centre.y, y0, y0 + 0.5),
                        dz = centre.z - Math.clamp(centre.z, z0, z0 + 0.5);
                if (dx * dx + dy * dy + dz * dz < r * r)
                    c.octants &= ~(1 << i);
            }
        });
    }

    /** Marks voxel {@code (x, y, z)} (frame-local, sixteenths) and its octant, and adds {@code kpg}. */
    private static void mark(Map<BlockPos, Cell> out, int x, int y, int z, double kpg) {
        BlockPos block = new BlockPos(Math.floorDiv(x, G), Math.floorDiv(y, G), Math.floorDiv(z, G));
        int ix = Math.floorMod(x, G), iy = Math.floorMod(y, G), iz = Math.floorMod(z, G);
        Cell c = out.computeIfAbsent(block, b -> new Cell());
        c.voxels.set(ChassisCellBlockEntity.index(ix, iy, iz));
        c.octants |= 1 << (ix / (G / 2) + 2 * (iy / (G / 2)) + 4 * (iz / (G / 2)));
        c.mass += kpg;
    }

    /**
     * A plate's or tube's Sable mass, {@code volume} in blocks; never zero. Its material's density
     * times its volume (a block is 1000 mB), read from the material system. A block no material
     * claims, or one nobody has weighed, falls back to Sable's own per-block mass.
     */
    public static double mass(double volume, BlockState material) {
        var found = Material.find(MaterialTable.get().materials(), material.getBlock().asItem());
        if (found.isPresent() && found.get().material().density().muPerMb() > 0)
            return Math.max(1e-3, Units.kpg(new Mu(found.get().material().density().muPerMb() * (float) volume * 1000f)));
        double perBlock = PhysicsBlockPropertyHelper.getMass(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, material);
        return Math.max(1e-3, volume * perBlock);
    }

    /** Points along segment a-b no further than {@link #SAMPLE} apart, nudged a hair inward. */
    private static List<Vec3> samples(Vec3 a, Vec3 b) {
        int n = (int) Math.ceil(b.subtract(a).length() / SAMPLE) + 1;
        Vec3 mid = a.add(b).scale(0.5);
        List<Vec3> out = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            Vec3 p = a.add(b.subtract(a).scale((double) i / n));
            out.add(p.add(mid.subtract(p).scale(1e-6)));
        }
        return out;
    }

    /**
     * Points across triangle a-b-c no further than {@link #SAMPLE} apart, each nudged a hair toward
     * the centroid -- a plate whose edge lies exactly on a block boundary must not claim the block
     * on the far side of it.
     */
    private static List<Vec3> samples(Vec3 a, Vec3 b, Vec3 c) {
        int n = (int) Math.ceil(Math.max(b.subtract(a).length(), c.subtract(a).length()) / SAMPLE) + 1;
        Vec3 centroid = a.add(b).add(c).scale(1.0 / 3);
        List<Vec3> out = new ArrayList<>();
        for (int i = 0; i <= n; i++)
            for (int j = 0; i + j <= n; j++) {
                double u = (double) i / n, v = (double) j / n;
                Vec3 p = a.add(b.subtract(a).scale(u)).add(c.subtract(a).scale(v));
                out.add(p.add(centroid.subtract(p).scale(1e-6)));
            }
        return out;
    }


    /**
     * The parts of {@code frame} once cut at its pivots, largest first, keeping only parts with a
     * plate or tube -- bare construction lines are not a body. One part means nothing to cut.
     *
     * <p>Two non-pivot vertices on one edge are one side, so they are united; a plate or tube
     * belongs to the side of its non-pivot vertex. An edge between two pivots, with nothing on it
     * to join it to a side (a link rod between two bearings), is a part of its own.
     *
     * <p>A locked bearing does not cut. It holds its two sides rigid, and one body is steadier
     * than two held by a joint -- a chain of short shafts on locked joints, each its own small
     * body, was found jammed into the road in the driving test. Switching it to a hinge or ball
     * changes the cut, which the vehicle takes up when it is next lifted.
     */
    public static List<Part> parts(Frame frame) {
        Set<Integer> pivots = new HashSet<>();
        frame.pivots().forEach((v, p) -> {
            if (p.cuts())
                pivots.add(v);
        });
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

        // A side's id is its lowest non-pivot vertex, which no other side holds. Its lowest vertex
        // outright will not do: a bearing belongs to every side it joins, and when it was the
        // lowest on two of them both took its id, became one group, and the bearing was asked to
        // join that group to itself -- a crash Sable repeats on every load. A part made only of
        // pivots (a link rod between bearings) has no such vertex and counts down from -1.
        List<Long> pivotOnly = verts.keySet().stream().filter(k -> k instanceof Long).map(k -> (Long) k).sorted().toList();
        List<Part> out = new ArrayList<>();
        for (Object key : verts.keySet()) {
            int solids = plates.get(key).size();
            for (long e : edges.get(key))
                if (frame.tubes().containsKey(e))
                    solids++;
            if (solids > 0)
                out.add(new Part(key instanceof Long l ? -1 - pivotOnly.indexOf(l)
                        : verts.get(key).stream().filter(v -> !pivots.contains(v)).mapToInt(Integer::intValue).min().orElseThrow(),
                        verts.get(key), edges.get(key), plates.get(key), solids));
        }
        out.sort((x, y) -> x.solids() != y.solids() ? Integer.compare(y.solids(), x.solids()) : Integer.compare(x.id(), y.id()));
        return out;
    }

    /**
     * How many cutting bearings of {@code frame} have a solid on one side only and no weld to a block
     * on the other: they swing nothing, and the Lift says so rather than lift a hinge that does not hinge.
     */
    public static int bearingsWithoutFarSide(Frame frame) {
        List<Part> parts = parts(frame);
        int lonely = 0;
        for (Map.Entry<Integer, Frame.Pivot> e : frame.pivots().entrySet()) {
            int v = e.getKey();
            if (!e.getValue().cuts() || frame.anchors().containsKey(v))
                continue;
            if (parts.stream().filter(p -> p.vertices().contains(v)).count() < 2)
                lonely++;
        }
        return lonely;
    }

    private static boolean cutsAt(Frame frame, int vertex) {
        Frame.Pivot p = frame.pivots().get(vertex);
        return p != null && p.cuts();
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
