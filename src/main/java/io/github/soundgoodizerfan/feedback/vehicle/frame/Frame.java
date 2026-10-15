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

import io.github.soundgoodizerfan.feedback.core.unit.Vdrag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A welded chassis: vertices on a 1/16 grid, edges the Welder ran between them, and plates
 * where three edges close a triangle ({@code feedback_vehicles_spec.md} section 4).
 *
 * <h2>Triangles, not polygons</h2>
 * The spec says "closed loops of edges form plates". Only the triangle is decided by its edges
 * alone -- four vertices joined in a ring need not be flat, and a non-flat quad has no area
 * anyone could agree on. So a plate is a triangle, and a flat quad is two of them sharing a
 * diagonal, which is also how every renderer in existence already thinks about it.
 *
 * <h2>Coordinates</h2>
 * Vertex positions are integers in sixteenths of a block, relative to the frame block's own
 * origin, and stay within {@link #RANGE} of it. Ids are stable across removals, so a removal
 * never renumbers an edge.
 */
public final class Frame {

    /** Grid divisions per block. */
    public static final int GRID = 16;
    /** How far a vertex may sit from the frame block, in sixteenths: eight blocks each way. */
    public static final int RANGE = 8 * GRID;
    /** Plate thickness, in blocks. [OPEN] in the spec whether this is a thin/thick choice. */
    public static final float PLATE_THICKNESS = 1f / GRID;
    /**
     * Tube sizes, as a banded choice (section 3 of the philosophy): thin, medium and thick, as a
     * diameter in 1/16ths. Thicker is heavier and stiffer and harder to fit; nothing in between.
     */
    public static final int[] TUBE_SIZES = {2, 4, 6};
    /**
     * Tire sizes, banded like tubes: small, medium and large, as a radius in 1/16ths. A bigger tire
     * is a taller final drive -- more ground per turn of the hub -- and heavier to spin up.
     */
    public static final int[] TIRE_SIZES = {4, 6, 8};

    /**
     * A weld point is one 1/16 cell -- 16^3 to a block -- not a grid crossing. {@code x, y, z} index
     * the cell; {@link #local()} is its centre, so a point never sits on a cell edge.
     */
    public record Vertex(int x, int y, int z) {
        public Vec3 local() {
            return new Vec3((x + 0.5) / GRID, (y + 0.5) / GRID, (z + 0.5) / GRID);
        }

        public boolean inRange() {
            return Math.abs(x) <= RANGE && Math.abs(y) <= RANGE && Math.abs(z) <= RANGE;
        }

        public static Vertex snap(Vec3 local) {
            return new Vertex((int) Math.floor(local.x * GRID), (int) Math.floor(local.y * GRID),
                    (int) Math.floor(local.z * GRID));
        }
    }

    /** A tube's material and size, an index into {@link #TUBE_SIZES}. */
    public record Tube(ResourceLocation material, int size) {
        public BlockState materialState() {
            return BuiltInRegistries.BLOCK.get(material).defaultBlockState();
        }

        /** Diameter in blocks. */
        public double diameter() {
            return TUBE_SIZES[size] / (double) GRID;
        }
    }

    /**
     * What a vertex does when the frame becomes a body: {@code kind} is always a bearing kind
     * ({@link FrameJoint.Kind#bearing()}), {@code axis} a hinge's axis in this frame's space, and
     * {@code drives} as on {@link FrameJoint}.
     */
    public record Pivot(FrameJoint.Kind kind, Vec3 axis, boolean drives) {
        public Pivot withDrive(boolean drives) {
            return new Pivot(kind, axis, drives);
        }
    }

    /**
     * A hub ({@code feedback_vehicles_spec.md} section 5, step 4): a hinge point given a tire. Its
     * material sets the grip ({@code mechanical.friction}) and the weight, its size an index into
     * {@link #TIRE_SIZES}. A setting, not a part: the tire is a ray, never a body.
     */
    public record Tire(ResourceLocation material, int size) {
        public BlockState materialState() {
            return BuiltInRegistries.BLOCK.get(material).defaultBlockState();
        }

        /** Radius in blocks. */
        public double radius() {
            return TIRE_SIZES[size] / (double) GRID;
        }
    }

    public record Plate(int a, int b, int c, ResourceLocation material) {
        public BlockState materialState() {
            return BuiltInRegistries.BLOCK.get(material).defaultBlockState();
        }
    }

    private final Map<Integer, Vertex> vertices = new LinkedHashMap<>();
    private final Set<Long> edges = new HashSet<>();
    private final Map<Long, Plate> plates = new HashMap<>();
    /** Edges made solid: edge key to tube material. An edge without one is construction line only. */
    private final Map<Long, Tube> tubes = new HashMap<>();
    /**
     * Points set to be a bearing. Once assembled, the frame's boxes are cut at each one into groups
     * held off one another by a joint there ({@link FrameBodies}); the frame itself stays one.
     */
    private final Map<Integer, Pivot> pivots = new HashMap<>();
    /**
     * Points welded to a block of the vehicle's base, as that block's offset from the frame block --
     * the two are lifted together, so the offset holds. Nothing else attaches the frame to its body
     * ({@code feedback_vehicles_spec.md} section 4): a group of boxes with no anchor, and no
     * bearing to a group that has one, falls off.
     */
    private final Map<Integer, BlockPos> anchors = new HashMap<>();
    /** Hinge points that are hubs. A tire lives only on a hinge; un-hinging the point drops it. */
    private final Map<Integer, Tire> tires = new HashMap<>();
    private int nextId;

    // --- vertices ---------------------------------------------------------------------------

    public Map<Integer, Vertex> vertices() {
        return vertices;
    }

    /** Adds a vertex, or returns the id already at that grid point. */
    public int add(Vertex v) {
        int existing = idAt(v);
        if (existing >= 0)
            return existing;
        vertices.put(nextId, v);
        return nextId++;
    }

    public int idAt(Vertex v) {
        for (Map.Entry<Integer, Vertex> e : vertices.entrySet())
            if (e.getValue().equals(v))
                return e.getKey();
        return -1;
    }

    public void remove(int id) {
        if (vertices.remove(id) == null)
            return;
        pivots.remove(id);
        anchors.remove(id);
        tires.remove(id);
        edges.removeIf(key -> (int) (key >>> 32) == id || (int) (long) key == id);
        tubes.keySet().retainAll(edges);
        plates.values().removeIf(p -> p.a == id || p.b == id || p.c == id);
    }

    /**
     * Moves a vertex; its edges and plates follow, since they name it by id. Refused if the
     * target is out of range or already holds another vertex.
     */
    public boolean move(int id, Vertex to) {
        if (!vertices.containsKey(id) || !to.inRange())
            return false;
        int there = idAt(to);
        if (there >= 0 && there != id)
            return false;
        vertices.put(id, to);
        return true;
    }

    /**
     * Puts a vertex at the middle of edge {@code a-b} (snapped to the grid) and splits the edge in
     * two. Every plate on the edge splits too, keeping its material, so the surface is unchanged
     * until the new vertex is moved. Returns the new id, or -1 if there is no edge or no free grid
     * point strictly between its ends.
     */
    public int split(int a, int b) {
        if (!hasEdge(a, b))
            return -1;
        Vertex va = vertices.get(a), vb = vertices.get(b);
        Vertex mid = new Vertex(Math.floorDiv(va.x + vb.x, 2), Math.floorDiv(va.y + vb.y, 2), Math.floorDiv(va.z + vb.z, 2));
        if (mid.equals(va) || mid.equals(vb) || idAt(mid) >= 0)
            return -1;
        int m = add(mid);
        List<Plate> touching = new ArrayList<>();
        for (Plate p : plates.values())
            if (uses(p, a) && uses(p, b))
                touching.add(p);
        edges.remove(edgeKey(a, b));
        plates.values().removeAll(touching);
        edges.add(edgeKey(a, m));
        edges.add(edgeKey(m, b));
        Tube tube = tubes.remove(edgeKey(a, b));
        if (tube != null) {
            tubes.put(edgeKey(a, m), tube);
            tubes.put(edgeKey(m, b), tube);
        }
        for (Plate p : touching) {
            int c = p.a != a && p.a != b ? p.a : p.b != a && p.b != b ? p.b : p.c;
            edges.add(edgeKey(m, c));
            plates.put(triKey(a, m, c), new Plate(a, m, c, p.material));
            plates.put(triKey(m, b, c), new Plate(m, b, c, p.material));
        }
        return m;
    }

    // --- pivots -----------------------------------------------------------------------------

    public Map<Integer, Pivot> pivots() {
        return pivots;
    }

    /** Sets vertex {@code id} to be a bearing, or back to a plain point with {@code null}. */
    public void setPivot(int id, @org.jetbrains.annotations.Nullable Pivot pivot) {
        if (!vertices.containsKey(id))
            return;
        if (pivot == null)
            pivots.remove(id);
        else
            pivots.put(id, pivot);
        if (pivot == null || pivot.kind() != FrameJoint.Kind.HINGE)
            tires.remove(id);
    }

    // --- tires ------------------------------------------------------------------------------

    public Map<Integer, Tire> tires() {
        return tires;
    }

    /** Makes hinge point {@code id} a hub, or takes its tire off with {@code null}. Refused on any other point. */
    public boolean setTire(int id, @org.jetbrains.annotations.Nullable Tire tire) {
        Pivot pivot = pivots.get(id);
        if (tire != null && (pivot == null || pivot.kind() != FrameJoint.Kind.HINGE))
            return false;
        if (tire == null)
            tires.remove(id);
        else
            tires.put(id, tire);
        return true;
    }

    // --- anchors ----------------------------------------------------------------------------

    public Map<Integer, BlockPos> anchors() {
        return anchors;
    }

    /** Welds vertex {@code id} to the block at {@code offset} from the frame block, or unwelds it with {@code null}. */
    public void setAnchor(int id, @org.jetbrains.annotations.Nullable BlockPos offset) {
        if (!vertices.containsKey(id))
            return;
        if (offset == null)
            anchors.remove(id);
        else
            anchors.put(id, offset.immutable());
    }

    // --- edges ------------------------------------------------------------------------------

    public Collection<Long> edgeKeys() {
        return edges;
    }

    public static int edgeA(long key) {
        return (int) (key >>> 32);
    }

    public static int edgeB(long key) {
        return (int) key;
    }

    public boolean hasEdge(int a, int b) {
        return edges.contains(edgeKey(a, b));
    }

    /**
     * Welds an edge, or cuts it if already welded. Cutting takes every plate that used it.
     * Returns whether the edge is present afterwards.
     */
    public boolean toggleEdge(int a, int b) {
        if (a == b || !vertices.containsKey(a) || !vertices.containsKey(b))
            return false;
        long key = edgeKey(a, b);
        if (edges.remove(key)) {
            plates.values().removeIf(p -> uses(p, a) && uses(p, b));
            tubes.remove(key);
            return false;
        }
        edges.add(key);
        return true;
    }

    /** The third vertices that close a triangle with edge {@code a-b}. */
    public List<Integer> closing(int a, int b) {
        List<Integer> out = new ArrayList<>();
        for (int c : vertices.keySet())
            if (c != a && c != b && hasEdge(a, c) && hasEdge(b, c))
                out.add(c);
        return out;
    }

    // --- tubes ------------------------------------------------------------------------------

    /** Tubes by edge key ({@link #edgeA}/{@link #edgeB}). */
    public Map<Long, Tube> tubes() {
        return tubes;
    }

    @org.jetbrains.annotations.Nullable
    public Tube tube(int a, int b) {
        return tubes.get(edgeKey(a, b));
    }

    /** Makes edge a-b a thin tube of {@code material}, or re-makes it in that material at its size; false if there is no such edge. */
    public boolean setTube(int a, int b, ResourceLocation material) {
        if (!hasEdge(a, b))
            return false;
        Tube was = tubes.get(edgeKey(a, b));
        tubes.put(edgeKey(a, b), new Tube(material, was == null ? 0 : was.size()));
        return true;
    }

    /** Steps tube a-b to the next size, thick wrapping back to thin. False if there is no tube. */
    public boolean thicken(int a, int b) {
        Tube was = tubes.get(edgeKey(a, b));
        if (was == null)
            return false;
        tubes.put(edgeKey(a, b), new Tube(was.material(), (was.size() + 1) % TUBE_SIZES.length));
        return true;
    }

    public boolean removeTube(int a, int b) {
        return tubes.remove(edgeKey(a, b)) != null;
    }

    /** The tubes meeting at vertex {@code id}, as edge keys. */
    public List<Long> tubesAt(int id) {
        List<Long> out = new ArrayList<>();
        for (long key : tubes.keySet())
            if (edgeA(key) == id || edgeB(key) == id)
                out.add(key);
        return out;
    }

    /** Solid volume, as a square section of the tube's diameter. */
    public double tubeVolume(long key) {
        double d = tubes.get(key).diameter();
        return pos(edgeA(key)).distanceTo(pos(edgeB(key))) * d * d;
    }

    // --- plates -----------------------------------------------------------------------------

    public Collection<Plate> plates() {
        return plates.values();
    }

    public boolean hasPlate(int a, int b, int c) {
        return plates.containsKey(triKey(a, b, c));
    }

    /** Lays a plate on a closed triangle. Refused if any edge is missing. */
    public boolean addPlate(int a, int b, int c, ResourceLocation material) {
        if (!hasEdge(a, b) || !hasEdge(b, c) || !hasEdge(a, c))
            return false;
        plates.put(triKey(a, b, c), new Plate(a, b, c, material));
        return true;
    }

    @org.jetbrains.annotations.Nullable
    public Plate plate(int a, int b, int c) {
        return plates.get(triKey(a, b, c));
    }

    public boolean removePlate(int a, int b, int c) {
        return plates.remove(triKey(a, b, c)) != null;
    }

    /** Every closed triangle of edges, plated or not, as id triples. */
    public List<int[]> triangles() {
        List<int[]> out = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (long key : edges) {
            int a = edgeA(key), b = edgeB(key);
            for (int c : closing(a, b))
                if (seen.add(triKey(a, b, c)))
                    out.add(new int[]{a, b, c});
        }
        return out;
    }

    public Vec3 pos(int id) {
        return vertices.get(id).local();
    }

    /** Twice-area vector (normal times twice the area), in block². */
    public Vec3 cross(Plate p) {
        Vec3 a = pos(p.a);
        return pos(p.b).subtract(a).cross(pos(p.c).subtract(a));
    }

    public double area(Plate p) {
        return cross(p).length() / 2;
    }

    /** Plate volume in blocks³ -- area times {@link #PLATE_THICKNESS}. */
    public double volume(Plate p) {
        return area(p) * PLATE_THICKNESS;
    }

    // --- aerodynamics -----------------------------------------------------------------------

    /**
     * Angle-weighted projected area seen moving along {@code direction}
     * ({@code feedback_vehicles_spec.md} section 4).
     *
     * <h2>Why rasterised, rather than summing each plate's projection</h2>
     * Summing {@code area x |cos|} over every plate counts a closed body twice -- the nose and the
     * tail both project onto the same patch of sky -- and halving it is wrong again for a single
     * flat plate. What the air actually meets is the <em>silhouette</em>: the union of everything
     * in front. So the projection is rasterised at grid resolution, each cell keeps only the
     * frontmost plate, and that plate's {@code |cos|} weights the cell -- a raked surface turns the
     * air aside rather than stopping it. A slab front and a wedge front with the same silhouette
     * come out genuinely different, which is the whole reason the shape is modelled at all.
     *
     * <p>Run once, when the chassis changes. Never per tick.
     */
    public Vdrag vdrag(Vec3 direction) {
        Vec3 d = direction.normalize();
        Vec3 u = (Math.abs(d.y) < 0.9 ? new Vec3(0, 1, 0) : new Vec3(1, 0, 0)).cross(d).normalize();
        Vec3 v = d.cross(u);

        record Projected(double[] us, double[] vs, double[] ds, double weight) {
        }
        List<Projected> tris = new ArrayList<>();
        double minU = Double.MAX_VALUE, minV = Double.MAX_VALUE, maxU = -Double.MAX_VALUE, maxV = -Double.MAX_VALUE;
        for (Plate p : plates.values()) {
            Vec3 n = cross(p);
            double len = n.length();
            if (len == 0)
                continue;
            int[] ids = {p.a, p.b, p.c};
            double[] us = new double[3], vs = new double[3], ds = new double[3];
            for (int i = 0; i < 3; i++) {
                Vec3 q = pos(ids[i]);
                us[i] = q.dot(u);
                vs[i] = q.dot(v);
                ds[i] = q.dot(d);
                minU = Math.min(minU, us[i]);
                maxU = Math.max(maxU, us[i]);
                minV = Math.min(minV, vs[i]);
                maxV = Math.max(maxV, vs[i]);
            }
            tris.add(new Projected(us, vs, ds, Math.abs(n.dot(d)) / len));
        }
        if (tris.isEmpty())
            return new Vdrag(0f);

        double cell = 1.0 / GRID;
        int w = (int) Math.ceil((maxU - minU) / cell) + 1;
        int h = (int) Math.ceil((maxV - minV) / cell) + 1;
        double[] depth = new double[w * h];
        double[] weight = new double[w * h];
        Arrays.fill(depth, Double.MAX_VALUE);

        for (Projected t : tris) {
            double denom = (t.vs[1] - t.vs[2]) * (t.us[0] - t.us[2]) + (t.us[2] - t.us[1]) * (t.vs[0] - t.vs[2]);
            if (Math.abs(denom) < 1e-12)
                continue; // edge-on: no silhouette
            int i0 = (int) ((Math.min(t.us[0], Math.min(t.us[1], t.us[2])) - minU) / cell);
            int i1 = (int) ((Math.max(t.us[0], Math.max(t.us[1], t.us[2])) - minU) / cell);
            int j0 = (int) ((Math.min(t.vs[0], Math.min(t.vs[1], t.vs[2])) - minV) / cell);
            int j1 = (int) ((Math.max(t.vs[0], Math.max(t.vs[1], t.vs[2])) - minV) / cell);
            for (int i = i0; i <= i1 && i < w; i++)
                for (int j = j0; j <= j1 && j < h; j++) {
                    double pu = minU + (i + 0.5) * cell, pv = minV + (j + 0.5) * cell;
                    double l0 = ((t.vs[1] - t.vs[2]) * (pu - t.us[2]) + (t.us[2] - t.us[1]) * (pv - t.vs[2])) / denom;
                    double l1 = ((t.vs[2] - t.vs[0]) * (pu - t.us[2]) + (t.us[0] - t.us[2]) * (pv - t.vs[2])) / denom;
                    double l2 = 1 - l0 - l1;
                    if (l0 < 0 || l1 < 0 || l2 < 0)
                        continue;
                    double z = l0 * t.ds[0] + l1 * t.ds[1] + l2 * t.ds[2];
                    int k = i * h + j;
                    // Frontmost along the direction of travel is the largest d.
                    if (depth[k] == Double.MAX_VALUE || z > depth[k]) {
                        depth[k] = z;
                        weight[k] = t.weight;
                    }
                }
        }

        double total = 0;
        for (double wk : weight)
            total += wk;
        return new Vdrag((float) (total * cell * cell));
    }

    // --- keys -------------------------------------------------------------------------------

    private static boolean uses(Plate p, int id) {
        return p.a == id || p.b == id || p.c == id;
    }

    static long edgeKey(int a, int b) {
        return ((long) Math.min(a, b) << 32) | (Math.max(a, b) & 0xffffffffL);
    }

    private static long triKey(int a, int b, int c) {
        int[] s = {a, b, c};
        Arrays.sort(s);
        return ((long) s[0] << 42) | ((long) s[1] << 21) | s[2];
    }

    // --- persistence ------------------------------------------------------------------------

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        ListTag vs = new ListTag();
        vertices.forEach((id, v) -> {
            CompoundTag e = new CompoundTag();
            e.putInt("Id", id);
            e.putIntArray("P", new int[]{v.x, v.y, v.z});
            vs.add(e);
        });
        tag.put("Vertices", vs);
        tag.putLongArray("Edges", edges.stream().mapToLong(Long::longValue).toArray());
        ListTag ps = new ListTag();
        plates.values().forEach(p -> {
            CompoundTag e = new CompoundTag();
            e.putIntArray("V", new int[]{p.a, p.b, p.c});
            e.putString("Material", p.material.toString());
            ps.add(e);
        });
        tag.put("Plates", ps);
        ListTag ts = new ListTag();
        tubes.forEach((key, tube) -> {
            CompoundTag e = new CompoundTag();
            e.putLong("Edge", key);
            e.putString("Material", tube.material().toString());
            e.putInt("Size", tube.size());
            ts.add(e);
        });
        tag.put("Tubes", ts);
        ListTag pv = new ListTag();
        pivots.forEach((id, p) -> {
            CompoundTag e = new CompoundTag();
            e.putInt("Id", id);
            e.putString("Kind", p.kind().name());
            e.putDouble("AxisX", p.axis().x);
            e.putDouble("AxisY", p.axis().y);
            e.putDouble("AxisZ", p.axis().z);
            e.putBoolean("Drives", p.drives());
            pv.add(e);
        });
        tag.put("Pivots", pv);
        ListTag an = new ListTag();
        anchors.forEach((id, off) -> {
            CompoundTag e = new CompoundTag();
            e.putInt("Id", id);
            e.putIntArray("Offset", new int[]{off.getX(), off.getY(), off.getZ()});
            an.add(e);
        });
        tag.put("Anchors", an);
        ListTag ti = new ListTag();
        tires.forEach((id, t) -> {
            CompoundTag e = new CompoundTag();
            e.putInt("Id", id);
            e.putString("Material", t.material().toString());
            e.putInt("Size", t.size());
            ti.add(e);
        });
        tag.put("Tires", ti);
        tag.putInt("Next", nextId);
        return tag;
    }

    public void load(CompoundTag tag) {
        vertices.clear();
        edges.clear();
        plates.clear();
        tubes.clear();
        pivots.clear();
        anchors.clear();
        tires.clear();
        for (Tag t : tag.getList("Vertices", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            int[] p = e.getIntArray("P");
            if (p.length == 3)
                vertices.put(e.getInt("Id"), new Vertex(p[0], p[1], p[2]));
        }
        for (long key : tag.getLongArray("Edges"))
            edges.add(key);
        for (Tag t : tag.getList("Plates", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            int[] v = e.getIntArray("V");
            ResourceLocation material = ResourceLocation.tryParse(e.getString("Material"));
            if (v.length == 3 && material != null)
                plates.put(triKey(v[0], v[1], v[2]), new Plate(v[0], v[1], v[2], material));
        }
        for (Tag t : tag.getList("Tubes", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            ResourceLocation material = ResourceLocation.tryParse(e.getString("Material"));
            if (material != null && edges.contains(e.getLong("Edge")))
                tubes.put(e.getLong("Edge"), new Tube(material, Math.floorMod(e.getInt("Size"), TUBE_SIZES.length)));
        }
        for (Tag t : tag.getList("Pivots", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            FrameJoint.Kind kind;
            try {
                kind = FrameJoint.Kind.valueOf(e.getString("Kind"));
            } catch (IllegalArgumentException bad) {
                continue;
            }
            Vec3 axis = new Vec3(e.getDouble("AxisX"), e.getDouble("AxisY"), e.getDouble("AxisZ"));
            if (kind.bearing() && vertices.containsKey(e.getInt("Id")))
                pivots.put(e.getInt("Id"), new Pivot(kind, axis.lengthSqr() < 1e-9 ? new Vec3(0, 1, 0) : axis.normalize(),
                        e.getBoolean("Drives")));
        }
        for (Tag t : tag.getList("Anchors", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            int[] o = e.getIntArray("Offset");
            if (o.length == 3 && vertices.containsKey(e.getInt("Id")))
                anchors.put(e.getInt("Id"), new BlockPos(o[0], o[1], o[2]));
        }
        for (Tag t : tag.getList("Tires", Tag.TAG_COMPOUND)) {
            CompoundTag e = (CompoundTag) t;
            ResourceLocation material = ResourceLocation.tryParse(e.getString("Material"));
            Pivot pivot = pivots.get(e.getInt("Id"));
            if (material != null && pivot != null && pivot.kind() == FrameJoint.Kind.HINGE)
                tires.put(e.getInt("Id"), new Tire(material, Math.floorMod(e.getInt("Size"), TIRE_SIZES.length)));
        }
        nextId = tag.getInt("Next");
    }
}
