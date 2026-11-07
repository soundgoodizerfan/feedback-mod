// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.village;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The walkable road tiles reachable from a point: a bounded flood along surface whose floor is in {@code feedback:road_surface}. A street
 * reaches farther than a lawn because the flood follows it; the player sees the road and so does the villager, so no figure is tuned.
 * Immutable once built; a Counter rebuilds it on a slow cadence.
 */
public final class RoadReach {

    public static final TagKey<Block> ROAD = TagKey.create(Registries.BLOCK, Feedback.id("road_surface"));
    public static final RoadReach NONE = new RoadReach(new LongOpenHashSet(), BlockPos.ZERO, BlockPos.ZERO);

    private final LongOpenHashSet tiles;
    private final BlockPos min, max;

    private RoadReach(LongOpenHashSet tiles, BlockPos min, BlockPos max) {
        this.tiles = tiles;
        this.min = min;
        this.max = max;
    }

    public int size() {
        return tiles.size();
    }

    /** The smallest box holding every tile, grown by the margin: the entity query's extent. */
    public net.minecraft.world.phys.AABB bounds() {
        return new net.minecraft.world.phys.AABB(min).minmax(new net.minecraft.world.phys.AABB(max)).inflate(VillageTuning.ROAD_MARGIN + 1);
    }

    /** The reached tile farthest from {@code from} (by squared distance), or null when there are none: where a courier is sent. */
    @org.jetbrains.annotations.Nullable
    public BlockPos farthest(BlockPos from) {
        BlockPos best = null;
        double bd = -1;
        for (long l : tiles) {
            BlockPos p = BlockPos.of(l);
            double d = p.distSqr(from);
            if (d > bd) {
                bd = d;
                best = p;
            }
        }
        return best;
    }

    /** Tiles with open sky above them. */
    public int openToSky(Level level) {
        int n = 0;
        for (long l : tiles)
            if (level.canSeeSky(BlockPos.of(l)))
                n++;
        return n;
    }

    /** Tiles whose eight horizontal neighbours are all reached tiles too: the heart of a square, not the line of a street. */
    public int interior() {
        int n = 0;
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        for (long l : tiles) {
            BlockPos p = BlockPos.of(l);
            boolean all = true;
            for (int dx = -1; dx <= 1 && all; dx++)
                for (int dz = -1; dz <= 1 && all; dz++)
                    if ((dx != 0 || dz != 0) && !tiles.contains(q.set(p.getX() + dx, p.getY(), p.getZ() + dz).asLong()))
                        all = false;
            if (all)
                n++;
        }
        return n;
    }

    /** Whether {@code p} stands within the margin of a reached tile. */
    public boolean near(BlockPos p) {
        if (tiles.isEmpty())
            return false;
        int m = VillageTuning.ROAD_MARGIN;
        BlockPos.MutableBlockPos q = new BlockPos.MutableBlockPos();
        for (int dx = -m; dx <= m; dx++)
            for (int dz = -m; dz <= m; dz++)
                for (int dy = -2; dy <= 1; dy++)
                    if (tiles.contains(q.set(p.getX() + dx, p.getY() + dy, p.getZ() + dz).asLong()))
                        return true;
        return false;
    }

    /** Floods from {@code start} (the tile in front of the shop face) along road surface, bounded by depth and tile count. */
    public static RoadReach flood(Level level, BlockPos start) {
        LongOpenHashSet seen = new LongOpenHashSet();
        java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
        java.util.ArrayDeque<Integer> depth = new java.util.ArrayDeque<>();
        BlockPos first = surface(level, start);
        if (first == null)
            return NONE;
        seen.add(first.asLong());
        queue.add(first);
        depth.add(0);
        int x0 = first.getX(), y0 = first.getY(), z0 = first.getZ(), x1 = x0, y1 = y0, z1 = z0;
        while (!queue.isEmpty() && seen.size() < VillageTuning.ROAD_FLOOD_MAX) {
            BlockPos p = queue.poll();
            int d = depth.poll();
            if (d >= VillageTuning.ROAD_REACH)
                continue;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos n = surface(level, p.relative(dir).above(dy));
                    if (n == null || !seen.add(n.asLong()))
                        continue;
                    queue.add(n);
                    depth.add(d + 1);
                    x0 = Math.min(x0, n.getX());
                    x1 = Math.max(x1, n.getX());
                    y0 = Math.min(y0, n.getY());
                    y1 = Math.max(y1, n.getY());
                    z0 = Math.min(z0, n.getZ());
                    z1 = Math.max(z1, n.getZ());
                }
            }
        }
        return new RoadReach(seen, new BlockPos(x0, y0, z0), new BlockPos(x1, y1, z1));
    }

    /** {@code p} if it is a standing tile (open space over a road block), the tile above a road block at {@code p}'s height otherwise, else null. */
    private static BlockPos surface(Level level, BlockPos p) {
        if (!level.isLoaded(p))
            return null;
        BlockState floor = level.getBlockState(p.below());
        if (floor.is(ROAD) && level.getBlockState(p).getCollisionShape(level, p).isEmpty())
            return p.immutable();
        return null;
    }
}
