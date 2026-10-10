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
import dev.ryanhcode.sable.physics.config.block_properties.PhysicsBlockPropertyHelper;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import io.github.soundgoodizerfan.feedback.registry.FVehicles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns a welded {@link Frame} into a Sable body ({@code feedback_vehicles_spec.md} section 4,
 * <em>Bridging to Sable</em>): every block a plate crosses gets a {@link ChassisCellBlock} shaped
 * like that plate at 1/16, then the frame block and its cells -- and nothing else -- are lifted
 * out as one body. An explicit block list rather than a floodfill, so a chassis parked on the
 * ground does not take the ground with it.
 */
public final class FrameAssembly {

    /** Sample spacing across a plate, in blocks. Finer than a voxel so no cell a plate crosses is missed. */
    private static final double SAMPLE = 1.0 / 32;
    /** Sable mass per {@link ChassisCellBlock#LOAD} step. [OPEN] tuning; matches the cell table. */
    public static final double MASS_PER_LOAD = 0.02;

    public enum Result { ASSEMBLED, NOTHING_TO_ASSEMBLE, BLOCKED }

    private record Cell(int orient, int offset) {
    }

    private FrameAssembly() {
    }

    /** The cells a frame's plates occupy, relative to the frame block, with their states. */
    public static Map<BlockPos, BlockState> cells(Frame frame) {
        Map<BlockPos, Cell> shape = new HashMap<>();
        Map<BlockPos, Boolean> mixed = new HashMap<>();
        Map<BlockPos, Double> mass = new HashMap<>();

        for (Frame.Plate p : frame.plates()) {
            Vec3 a = frame.pos(p.a()), b = frame.pos(p.b()), c = frame.pos(p.c());
            Vec3 normal = frame.cross(p);
            if (normal.lengthSqr() == 0)
                continue;
            int orient = ChassisCellBlock.orient(normal);
            double plateMass = frame.volume(p) * materialMass(p.materialState());

            List<Vec3> samples = samples(a, b, c);
            Map<BlockPos, Integer> hits = new HashMap<>();
            for (Vec3 s : samples)
                hits.merge(BlockPos.containing(s), 1, Integer::sum);

            for (Map.Entry<BlockPos, Integer> e : hits.entrySet()) {
                BlockPos pos = e.getKey();
                Vec3 local = a.subtract(Vec3.atLowerCornerOf(pos));
                Cell cell = new Cell(orient, ChassisCellBlock.offset(orient, local));
                Cell prior = shape.putIfAbsent(pos, cell);
                if (prior != null && !prior.equals(cell))
                    mixed.put(pos, true);
                mass.merge(pos, plateMass * e.getValue() / samples.size(), Double::sum);
            }
        }

        BlockState base = FVehicles.CHASSIS_CELL.get().defaultBlockState();
        Map<BlockPos, BlockState> out = new HashMap<>();
        shape.forEach((pos, cell) -> {
            boolean full = mixed.containsKey(pos);
            int load = (int) Math.max(1, Math.min(ChassisCellBlock.LOADS - 1, Math.round(mass.get(pos) / MASS_PER_LOAD)));
            out.put(pos, base
                    .setValue(ChassisCellBlock.ORIENT, full ? ChassisCellBlock.FULL : cell.orient)
                    .setValue(ChassisCellBlock.OFFSET, full ? 0 : cell.offset)
                    .setValue(ChassisCellBlock.LOAD, load));
        });
        return out;
    }

    /**
     * Places the cells and lifts frame plus cells into one Sable body. Refuses, placing nothing,
     * if any cell would land on a block that is not air.
     */
    @Nullable
    public static ServerSubLevel assemble(ServerLevel level, BlockPos framePos, FrameBlockEntity be, Result[] result) {
        Frame frame = be.frame();
        Map<BlockPos, BlockState> local = cells(frame);
        local.remove(BlockPos.ZERO); // the frame block's own cell
        if (local.isEmpty()) {
            result[0] = Result.NOTHING_TO_ASSEMBLE;
            return null;
        }
        for (BlockPos rel : local.keySet())
            if (!level.getBlockState(framePos.offset(rel)).isAir()) {
                result[0] = Result.BLOCKED;
                return null;
            }

        be.cells().clear();
        be.cells().addAll(local.keySet());
        be.setChanged();
        List<BlockPos> blocks = new ArrayList<>();
        blocks.add(framePos);
        int x0 = framePos.getX(), y0 = framePos.getY(), z0 = framePos.getZ(), x1 = x0, y1 = y0, z1 = z0;
        for (Map.Entry<BlockPos, BlockState> e : local.entrySet()) {
            BlockPos at = framePos.offset(e.getKey());
            level.setBlock(at, e.getValue(), 3);
            blocks.add(at);
            x0 = Math.min(x0, at.getX());
            y0 = Math.min(y0, at.getY());
            z0 = Math.min(z0, at.getZ());
            x1 = Math.max(x1, at.getX());
            y1 = Math.max(y1, at.getY());
            z1 = Math.max(z1, at.getZ());
        }
        result[0] = Result.ASSEMBLED;
        return SubLevelAssemblyHelper.assembleBlocks(level, framePos, blocks, new BoundingBox3i(x0, y0, z0, x1, y1, z1));
    }

    /**
     * Re-cuts an assembled frame's cells after an edit: old cells out, new cells in, all inside the
     * body it already is. Sable sees ordinary block changes and updates collision and mass itself.
     * A new cell that would land on some other block is skipped -- the plate still renders, it
     * just has a gap in its collision there -- rather than refusing an edit to a moving vehicle.
     */
    public static void recut(ServerLevel level, BlockPos framePos, FrameBlockEntity be) {
        Map<BlockPos, BlockState> local = cells(be.frame());
        local.remove(BlockPos.ZERO);
        for (BlockPos rel : be.cells())
            if (!local.containsKey(rel) && level.getBlockState(framePos.offset(rel)).is(FVehicles.CHASSIS_CELL.get()))
                level.setBlock(framePos.offset(rel), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
        be.cells().clear();
        for (Map.Entry<BlockPos, BlockState> e : local.entrySet()) {
            BlockPos at = framePos.offset(e.getKey());
            BlockState there = level.getBlockState(at);
            if (there.isAir() || there.is(FVehicles.CHASSIS_CELL.get())) {
                if (!there.equals(e.getValue()))
                    level.setBlock(at, e.getValue(), 3);
                be.cells().add(e.getKey());
            }
        }
    }

    /** A plate material's Sable mass for a full block -- a property every block already has. */
    private static double materialMass(BlockState state) {
        return PhysicsBlockPropertyHelper.getMass(EmptyBlockGetter.INSTANCE, BlockPos.ZERO, state);
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
}
