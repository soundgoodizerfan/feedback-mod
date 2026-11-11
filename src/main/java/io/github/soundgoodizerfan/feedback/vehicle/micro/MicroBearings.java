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
package io.github.soundgoodizerfan.feedback.vehicle.micro;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.math.BoundingBox3i;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.storage.SubLevelRemovalReason;
import io.github.soundgoodizerfan.feedback.core.FTuning;
import io.github.soundgoodizerfan.feedback.core.rotation.RotationPropagator;
import io.github.soundgoodizerfan.feedback.machine.bearing.Joint;
import io.github.soundgoodizerfan.feedback.machine.bearing.JointBlock;
import io.github.soundgoodizerfan.feedback.machine.bearing.JointPart;
import io.github.soundgoodizerfan.feedback.machine.bearing.RotaryBearingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Micro bearings ({@code docs/specs/feedback_micro_spec.md}, <em>Micro bearings and ring gears</em>): a
 * Rotary Bearing or Ring Gear voxel turns the voxels connected to its front face as a Sable body.
 *
 * <p>The voxel-level split, on the stock {@code assembleBlocks}: every cell holding a moving voxel
 * is assembled whole (so Sable makes the body, its pose and its physics as for any build, and every
 * voxel keeps its place), then the voxels of those cells that were not moving are taken out of the
 * body's cells and put back into the world at the same positions. Putting back turns every voxel's
 * home position by the nearest quarter turn about the bearing voxel's centre line -- exact on the
 * voxel grid -- and its {@code axis} and {@code Direction} properties with it.
 */
public final class MicroBearings {

    private static final int N = MicroCellBlockEntity.N;

    private MicroBearings() {
    }

    /** Assembles, or puts back, for the bearing voxel {@code index} of the cell at {@code cellPos}. Returns the message key. */
    public static String toggle(ServerLevel level, BlockPos cellPos, int index) {
        if (!(level.getBlockEntity(cellPos) instanceof MicroCellBlockEntity cell))
            return "empty";
        Joint joint = cell.bearing(index);
        return joint == null ? assemble(level, cell, index) : putBack(level, cell, index, joint);
    }

    private static BlockPos voxel(BlockPos cell, int index) {
        return new BlockPos(cell.getX() * N + (index & 3), cell.getY() * N + (index >> 2 & 3), cell.getZ() * N + (index >> 4));
    }

    private static int indexOf(BlockPos v) {
        return MicroCellBlockEntity.index(Math.floorMod(v.getX(), N), Math.floorMod(v.getY(), N), Math.floorMod(v.getZ(), N));
    }

    @Nullable
    private static BlockState at(ServerLevel level, BlockPos v) {
        BlockPos c = MicroPlacer.cellOf(v.getX(), v.getY(), v.getZ());
        return level.isLoaded(c) && level.getBlockEntity(c) instanceof MicroCellBlockEntity cell ? cell.get(indexOf(v)) : null;
    }

    private static String assemble(ServerLevel level, MicroCellBlockEntity cell, int index) {
        BlockPos cellPos = cell.getBlockPos();
        // From a body too: Sable starts the new body at its parent's pose and speed.
        ServerSubLevel parent = Joint.parentOf(level, cellPos);
        Direction f = cell.get(index).getValue(RotaryBearingBlock.FACING);
        Joint.Kind kind = ((JointPart) cell.get(index).getBlock()).kind();
        int variant = cell.get(index).hasProperty(JointBlock.VARIANT) ? cell.get(index).getValue(JointBlock.VARIANT) : 1;
        BlockPos b = voxel(cellPos, index), front = b.relative(f), host = b.relative(f.getOpposite());
        if (at(level, front) == null)
            return "empty";
        // The moving voxels: connected through faces to the one in front, never the bearing nor its host.
        Set<BlockPos> moving = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(List.of(front));
        moving.add(front);
        while (!queue.isEmpty()) {
            BlockPos v = queue.poll();
            for (Direction d : Direction.values()) {
                BlockPos n = v.relative(d);
                if (n.equals(b) || n.equals(host) || moving.contains(n) || at(level, n) == null)
                    continue;
                if (moving.size() >= FTuning.ROTARY_BEARING_MAX_VOXELS)
                    return "too_big";
                moving.add(n);
                queue.add(n);
            }
        }
        // By cell: what stays in each (put back into the world after the cell is assembled whole).
        Map<BlockPos, Map<Integer, BlockState>> staying = new LinkedHashMap<>();
        for (BlockPos v : moving)
            staying.computeIfAbsent(MicroPlacer.cellOf(v.getX(), v.getY(), v.getZ()), c -> new LinkedHashMap<>());
        for (var e : staying.entrySet())
            if (level.getBlockEntity(e.getKey()) instanceof MicroCellBlockEntity c)
                for (int i = 0; i < MicroCellBlockEntity.VOXELS; i++)
                    if (c.get(i) != null && !moving.contains(voxel(e.getKey(), i)))
                        e.getValue().put(i, c.get(i));
        List<BlockPos> cells = new ArrayList<>(staying.keySet());
        // What each world cell carries besides its voxels -- other bearings' holds, their clearances --
        // goes into the body with the whole cell, so it is taken now and given to the cell made anew.
        Map<BlockPos, Carried> carried = new LinkedHashMap<>();
        for (BlockPos c : cells)
            if (level.getBlockEntity(c) instanceof MicroCellBlockEntity w)
                carried.put(c, new Carried(new it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<>(w.bearings()), w.clear(), w.hinge()));
        ServerSubLevel made = SubLevelAssemblyHelper.assembleBlocks(level, cells.get(0), cells, BoundingBox3i.from(cells));
        // Clearance (see MicroCellBlockEntity#clear): in a cell split in two, the octants both halves
        // hold leave both colliders; the bearing voxel's octant leaves the stator's, and the voxel in
        // front's octant leaves the body's, so the hinge faces never touch.
        BlockPos frontCell = MicroPlacer.cellOf(front.getX(), front.getY(), front.getZ());
        Map<BlockPos, Integer> statorClear = new LinkedHashMap<>(), bodyClear = new LinkedHashMap<>();
        for (var e : staying.entrySet()) {
            long stays = 0, goes = 0;
            for (int i = 0; i < MicroCellBlockEntity.VOXELS; i++)
                if (e.getValue().containsKey(i))
                    stays |= 1L << i;
                else if (moving.contains(voxel(e.getKey(), i)))
                    goes |= 1L << i;
            int shared = MicroCellBlockEntity.octants(stays) & MicroCellBlockEntity.octants(goes);
            if (shared != 0) {
                statorClear.merge(e.getKey(), shared, (x, y) -> x | y);
                bodyClear.merge(e.getKey(), shared, (x, y) -> x | y);
            }
        }
        statorClear.merge(cellPos, MicroCellBlockEntity.octants(1L << index), (x, y) -> x | y);
        bodyClear.merge(frontCell, MicroCellBlockEntity.octants(1L << indexOf(front)), (x, y) -> x | y);
        for (var e : staying.entrySet()) {
            // The cell's place in the new body: through the world, since a parent's plot is not the world.
            Vec3 at = Vec3.atCenterOf(e.getKey());
            BlockPos plot = BlockPos.containing(made.logicalPose().transformPositionInverse(
                    parent == null ? at : parent.logicalPose().transformPosition(at)));
            if (level.getBlockEntity(plot) instanceof MicroCellBlockEntity inBody) {
                // The copies of the staying voxels' bearings go first: set would let them go and
                // clear the world bearings' clearances, which the copies name too.
                inBody.bearings().keySet().removeAll(e.getValue().keySet());
                for (int i : e.getValue().keySet())
                    inBody.set(i, null);
                inBody.commit();
                inBody.setClear(bodyClear.getOrDefault(e.getKey(), 0));
            }
            for (var v : e.getValue().entrySet()) {
                BlockPos w = voxel(e.getKey(), v.getKey());
                MicroPlacer.put(level, w.getX(), w.getY(), w.getZ(), v.getValue());
            }
        }
        // The cells made anew keep what they carried (other bearings, their clearances), minus what moved.
        for (var e : carried.entrySet())
            if (level.getBlockEntity(e.getKey()) instanceof MicroCellBlockEntity w) {
                Carried c = e.getValue();
                for (var held : c.bearings().int2ObjectEntrySet())
                    if (w.get(held.getIntKey()) != null)
                        w.setBearing(held.getIntKey(), held.getValue());
                w.setClear(w.clear() | c.clear());
                w.setHinge(c.hinge() & w.filled());
            }
        Joint joint = Joint.of(kind, variant, made, faceCentre(b, f), f, cells.get(0), parent);
        for (var e : statorClear.entrySet())
            if (level.getBlockEntity(e.getKey()) instanceof MicroCellBlockEntity stator) {
                stator.setClear(stator.clear() | e.getValue());
                joint.cleared.add(e.getKey());
            }
        // The hinge faces: the bearing voxel's on the stator, the front voxel's on the turned body.
        if (level.getBlockEntity(cellPos) instanceof MicroCellBlockEntity stator)
            stator.setHinge(stator.hinge() | 1L << index);
        Vec3 frontAt = Vec3.atCenterOf(frontCell);
        BlockPos frontPlot = BlockPos.containing(made.logicalPose().transformPositionInverse(
                parent == null ? frontAt : parent.logicalPose().transformPosition(frontAt)));
        if (level.getBlockEntity(frontPlot) instanceof MicroCellBlockEntity turned)
            turned.setHinge(turned.hinge() | 1L << indexOf(front));
        // The bearing's cell may have been made anew (it was assembled whole and put back): fetch it.
        if (level.getBlockEntity(cellPos) instanceof MicroCellBlockEntity home)
            home.setBearing(index, joint);
        else
            joint.letGo();
        return "assembled";
    }

    private static org.joml.Vector3d faceCentre(BlockPos v, Direction f) {
        return new org.joml.Vector3d((v.getX() + 0.5 + 0.5 * f.getStepX()) / N, (v.getY() + 0.5 + 0.5 * f.getStepY()) / N,
                (v.getZ() + 0.5 + 0.5 * f.getStepZ()) / N);
    }

    private static String putBack(ServerLevel level, MicroCellBlockEntity cell, int index, Joint joint) {
        ServerSubLevel s = joint.held(level);
        if (s == null)
            return "not_loaded";
        if (Joint.carriesChild(level, s))
            return "has_child";
        Direction f = cell.get(index).getValue(RotaryBearingBlock.FACING);
        Direction.Axis axis = f.getAxis();
        float sign = f.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : -1;
        // The body's angle about the axis's positive direction, to the nearest quarter turn; any other
        // joint can have turned about any axis, so its whole rotation is snapped to the nearest of the 24.
        boolean hinge = joint.kind() == Joint.Kind.HINGE;
        int k = hinge ? Math.floorMod(Math.round(sign * joint.turned(s, Joint.parentOf(level, cell.getBlockPos()), f) / 90f), 4) : 0;
        int[][] snapped = hinge ? null : snap(s, Joint.parentOf(level, cell.getBlockPos()));
        BlockPos b = voxel(cell.getBlockPos(), index);
        BlockPos offset = joint.homeTo().subtract(joint.homeFrom());
        Map<BlockPos, BlockState> moves = new LinkedHashMap<>();
        var box = s.getPlot().getBoundingBox();
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ()))
            if (level.getBlockEntity(p) instanceof MicroCellBlockEntity inBody)
                for (int i = 0; i < MicroCellBlockEntity.VOXELS; i++)
                    if (inBody.get(i) != null)
                        moves.put(hinge ? turn(voxel(p.offset(offset), i), b, axis, k) : turn(voxel(p.offset(offset), i), b, snapped),
                                hinge ? turn(inBody.get(i), axis, k) : turn(inBody.get(i), snapped));
        for (BlockPos v : moves.keySet()) {
            BlockPos c = MicroPlacer.cellOf(v.getX(), v.getY(), v.getZ());
            BlockState there = level.getBlockState(c);
            boolean free = level.getBlockEntity(c) instanceof MicroCellBlockEntity mc ? mc.get(indexOf(v)) == null
                    : there.isAir() || there.canBeReplaced() && there.getFluidState().isEmpty();
            if (!free)
                return "blocked";
        }
        joint.letGo();
        cell.setBearing(index, null);
        unclear(level, joint);
        ServerSubLevelContainer.getContainer(level).removeSubLevel(s, SubLevelRemovalReason.REMOVED);
        for (var m : moves.entrySet())
            MicroPlacer.put(level, m.getKey().getX(), m.getKey().getY(), m.getKey().getZ(), m.getValue());
        RotationPropagator.rebuildFrom(level, cell.getBlockPos());
        return "disassembled";
    }

    /**
     * The stator cells' colliders back as their voxels say, once the bearing holds nothing.
     * shortcut: clears the whole masks of those cells, so another bearing sharing one of them loses its
     * clearance there until it is re-assembled; with voxel colliders on, the octant mask is not what
     * collides, so this only matters when they are off.
     */
    public static void unclear(net.minecraft.world.level.Level level, Joint joint) {
        for (BlockPos p : joint.cleared)
            if (level.getBlockEntity(p) instanceof MicroCellBlockEntity c) {
                c.setClear(0);
                c.setHinge(0);
            }
        joint.cleared.clear();
    }

    /** What a world cell carries besides its voxels, kept across its being made anew. */
    private record Carried(it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap<Joint> bearings, int clear, long hinge) {
    }

    /**
     * The turn of body {@code s} against {@code parent} (null: the world) as the nearest of the 24
     * cube rotations: a signed permutation matrix, row i the axis and sign that world axis i comes from.
     * A body assembled from rest sits at its parent's pose, so the relative turn is the whole turn.
     */
    static int[][] snap(ServerSubLevel s, @Nullable ServerSubLevel parent) {
        org.joml.Quaterniond rel = new org.joml.Quaterniond(s.logicalPose().orientation());
        if (parent != null)
            rel = new org.joml.Quaterniond(parent.logicalPose().orientation()).conjugate().mul(rel);
        org.joml.Matrix3d r = new org.joml.Matrix3d().set(rel);
        double[][] a = {{r.m00, r.m10, r.m20}, {r.m01, r.m11, r.m21}, {r.m02, r.m12, r.m22}};
        int[][] m = new int[3][3];
        boolean[] used = new boolean[3];
        for (int i = 0; i < 3; i++) {
            int best = -1;
            for (int j = 0; j < 3; j++)
                if (!used[j] && (best < 0 || Math.abs(a[i][j]) > Math.abs(a[i][best])))
                    best = j;
            used[best] = true;
            m[i][best] = a[i][best] < 0 ? -1 : 1;
        }
        // A reflection is not a turn: flip one row's sign to make the determinant +1.
        int det = m[0][0] * (m[1][1] * m[2][2] - m[1][2] * m[2][1]) - m[0][1] * (m[1][0] * m[2][2] - m[1][2] * m[2][0])
                + m[0][2] * (m[1][0] * m[2][1] - m[1][1] * m[2][0]);
        if (det < 0)
            for (int j = 0; j < 3; j++)
                m[2][j] = -m[2][j];
        return m;
    }

    /** Voxel {@code v} turned about the centre of voxel {@code b} by the rotation {@code m} (rows from {@link #snap}). */
    static BlockPos turn(BlockPos v, BlockPos b, int[][] m) {
        int x = 2 * (v.getX() - b.getX()), y = 2 * (v.getY() - b.getY()), z = 2 * (v.getZ() - b.getZ());
        int nx = m[0][0] * x + m[0][1] * y + m[0][2] * z, ny = m[1][0] * x + m[1][1] * y + m[1][2] * z, nz = m[2][0] * x + m[2][1] * y + m[2][2] * z;
        return new BlockPos(b.getX() + nx / 2, b.getY() + ny / 2, b.getZ() + nz / 2);
    }

    /** {@code state} turned by {@code m}: its axis and Direction properties. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static BlockState turn(BlockState state, int[][] m) {
        for (Property<?> property : state.getProperties()) {
            Object value = state.getValue(property);
            Direction d = value instanceof Direction dir ? dir : value instanceof Direction.Axis a ? Direction.get(Direction.AxisDirection.POSITIVE, a) : null;
            if (d == null)
                continue;
            int x = d.getStepX(), y = d.getStepY(), z = d.getStepZ();
            Direction to = Direction.fromDelta(m[0][0] * x + m[0][1] * y + m[0][2] * z, m[1][0] * x + m[1][1] * y + m[1][2] * z,
                    m[2][0] * x + m[2][1] * y + m[2][2] * z);
            Object turned = to == null ? null : value instanceof Direction ? to : to.getAxis();
            if (turned != null && property.getPossibleValues().contains(turned))
                state = state.setValue((Property) property, (Comparable) turned);
        }
        return state;
    }

    /** Voxel {@code v} turned {@code k} quarter turns about {@code axis} through the centre of voxel {@code b}, right-handed. */
    static BlockPos turn(BlockPos v, BlockPos b, Direction.Axis axis, int k) {
        // Doubled coordinates, so the centre line is on the integer grid and the turn is exact.
        int x = 2 * (v.getX() - b.getX()), y = 2 * (v.getY() - b.getY()), z = 2 * (v.getZ() - b.getZ());
        for (int i = 0; i < k; i++) {
            int t;
            switch (axis) {
                case X -> { t = y; y = -z; z = t; }
                case Y -> { t = z; z = -x; x = t; }
                case Z -> { t = x; x = -y; y = t; }
            }
        }
        return new BlockPos(b.getX() + x / 2, b.getY() + y / 2, b.getZ() + z / 2);
    }

    /** {@code state} turned {@code k} quarter turns about {@code axis}: its axis and Direction properties. */
    static BlockState turn(BlockState state, Direction.Axis axis, int k) {
        if (k == 0)
            return state;
        for (Property<?> property : state.getProperties())
            state = turnProperty(state, property, axis, k);
        return state;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState turnProperty(BlockState state, Property property, Direction.Axis axis, int k) {
        Object value = state.getValue(property);
        Object turned;
        if (value instanceof Direction d) {
            BlockPos n = turn(new BlockPos(d.getStepX(), d.getStepY(), d.getStepZ()), BlockPos.ZERO, axis, k);
            turned = Direction.fromDelta(n.getX(), n.getY(), n.getZ());
        } else if (value instanceof Direction.Axis a) {
            Direction d = Direction.get(Direction.AxisDirection.POSITIVE, a);
            BlockPos n = turn(new BlockPos(d.getStepX(), d.getStepY(), d.getStepZ()), BlockPos.ZERO, axis, k);
            Direction to = Direction.fromDelta(n.getX(), n.getY(), n.getZ());
            turned = to == null ? null : to.getAxis();
        } else
            return state;
        return turned != null && property.getPossibleValues().contains(turned) ? state.setValue(property, (Comparable) turned) : state;
    }
}
