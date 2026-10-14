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
package io.github.soundgoodizerfan.feedback.core.electric;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Finds the connected run a node belongs to and hands it to the resolver. The
 * {@code RotationPropagator} pattern: when topology changes, flood the whole run once and
 * rebuild it; never patch one.
 *
 * <h2>Why a rebuild and not an incremental update</h2>
 * Same argument as the rotation network, and it is stronger here: a run's cached routing tree is
 * only as good as the graph it was built from, and keeping a tree correct across a wire being
 * broken in its middle is exactly the accumulated-ownership bug a rebuild cannot have. A flood
 * fill visits each node once and cannot loop, however the player wires a ring.
 *
 * <h2>Who is adjacent to whom</h2>
 * Touching blocks are connected unless they are the same non-wire role: a wire joins anything, a
 * source can feed a sink it touches directly, but two sinks or two sources touching conduct
 * nothing between them. Nothing is sided -- a wire connects on all six faces -- because routing a
 * cable round a corner is the busywork philosophy 3 cuts.
 */
public final class ElectricPropagator {

    private ElectricPropagator() {
    }

    /**
     * Queued, not rebuilt at once, for the same reason as the rotation propagator: a chunk
     * loading a thousand-wire run calls this a thousand times, and rebuilding each time floods
     * the whole run every call.
     */
    static void onAdded(Level level, BlockPos pos) {
        ElectricRuns.of(level).queueAdded(pos);
    }

    static void flushAdded(Level level, Set<BlockPos> added) {
        Set<BlockPos> covered = new HashSet<>();
        for (BlockPos pos : added)
            if (!covered.contains(pos))
                for (ElectricNode member : rebuild(level, pos))
                    covered.add(member.getBlockPos());
    }

    static void onRemoved(Level level, BlockPos pos, ElectricNode removed) {
        ElectricRuns runs = ElectricRuns.of(level);
        if (removed.getRun() != null)
            runs.discard(removed.getRun());
        removed.setRun(null);
        // The run may have been cut in two: rebuild from each neighbour, skipping any that an
        // earlier rebuild already covered.
        Set<BlockPos> covered = new HashSet<>();
        for (Direction face : Direction.values()) {
            BlockPos neighbour = pos.relative(face);
            if (covered.contains(neighbour) || nodeAt(level, neighbour) == null)
                continue;
            for (ElectricNode member : rebuild(level, neighbour))
                covered.add(member.getBlockPos());
        }
    }

    /** Re-resolve the run at {@code pos}. For tests and for anything that changes a node's role. */
    public static void rebuildFrom(Level level, BlockPos pos) {
        rebuild(level, pos);
    }

    private static List<ElectricNode> rebuild(Level level, BlockPos pos) {
        ElectricNode start = nodeAt(level, pos);
        if (start == null)
            return List.of();

        // Flood fill, keeping an index per node so adjacency is built in the same pass.
        List<ElectricNode> found = new ArrayList<>();
        Map<BlockPos, Integer> index = new HashMap<>();
        Deque<ElectricNode> queue = new ArrayDeque<>();
        queue.add(start);
        index.put(start.getBlockPos(), 0);
        found.add(start);
        List<List<Integer>> edges = new ArrayList<>();
        edges.add(new ArrayList<>());

        while (!queue.isEmpty()) {
            ElectricNode current = queue.poll();
            int ci = index.get(current.getBlockPos());
            for (Direction face : Direction.values()) {
                ElectricNode other = nodeAt(level, current.getBlockPos().relative(face));
                if (other == null || !connects(current, other))
                    continue;
                Integer oi = index.get(other.getBlockPos());
                if (oi == null) {
                    oi = found.size();
                    index.put(other.getBlockPos(), oi);
                    found.add(other);
                    edges.add(new ArrayList<>());
                    queue.add(other);
                }
                edges.get(ci).add(oi);
            }
        }

        int[][] adjacent = new int[found.size()][];
        for (int i = 0; i < adjacent.length; i++)
            adjacent[i] = edges.get(i).stream().mapToInt(Integer::intValue).toArray();

        ElectricRuns runs = ElectricRuns.of(level);
        for (ElectricNode node : found)
            if (node.getRun() != null)
                runs.discard(node.getRun());
        ElectricRun run = new ElectricRun(found, adjacent);
        runs.add(run);
        for (ElectricNode node : found)
            node.setRun(run);
        return found;
    }

    /**
     * A member started or stopped conducting: rebuild its own run and every neighbour's, since
     * opening splits one run into two and closing joins two into one.
     */
    public static void reconnect(Level level, BlockPos pos) {
        rebuild(level, pos);
        for (Direction face : Direction.values())
            if (nodeAt(level, pos.relative(face)) != null)
                rebuild(level, pos.relative(face));
    }

    private static boolean connects(ElectricMember a, ElectricMember b) {
        if (!a.conducting() || !b.conducting())
            return false;
        return a.role() == ElectricMember.Role.WIRE || b.role() == ElectricMember.Role.WIRE
                || a.role() != b.role();
    }

    @Nullable
    private static ElectricNode nodeAt(Level level, BlockPos pos) {
        if (!level.isLoaded(pos))
            return null;
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof ElectricNode node && !node.isGone() ? node : null;
    }
}
