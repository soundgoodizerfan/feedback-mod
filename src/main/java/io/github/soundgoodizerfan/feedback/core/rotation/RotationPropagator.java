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
package io.github.soundgoodizerfan.feedback.core.rotation;
import io.github.soundgoodizerfan.feedback.core.unit.Rpm;

import org.jetbrains.annotations.Nullable;
import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Works out how fast everything on a rotation network turns.
 *
 * <h2>Why this is a rebuild and not an incremental update</h2>
 * Create propagates incrementally: each node remembers which neighbour drives it, a faster
 * source "overpowers" a slower one, and changes ripple outward from wherever they happened.
 * That is the right call for Create, whose networks are enormous and whose propagation cost
 * would otherwise show up in the frame time.
 * <p>
 * We rebuild the whole connected run instead. Flood fill it, find the strongest source, walk
 * outward from that source assigning speeds. It is O(n) in the size of one shaft run rather
 * than O(size of the change), which is worse on paper and irrelevant in practice at the scale
 * a Feedback factory actually reaches -- and it is enormously easier to be sure it is correct,
 * because there is no accumulated ownership state to get out of step with the world.
 * <p>
 * It also deletes a whole bug class. Create needs a "flicker score" that breaks blocks whose
 * speed changes too often, because a network wired into a loop can propagate forever. A rebuild
 * visits each node once and cannot loop at all.
 * <p>
 * Design derived from Create's {@code RotationPropagator} (MIT) -- see THIRD-PARTY-LICENSES.md.
 */
public class RotationPropagator {

    /** A run with no source is not turning. */
    private static final Rpm STOPPED = new Rpm(0);

    /**
     * Queued, not rebuilt at once: a chunk loading a thousand-shaft run calls this a thousand
     * times, and rebuilding each one floods the whole run every time -- O(n²) on world load.
     * {@link #flushAdded} rebuilds each run once at the start of the next tick.
     */
    public static void onAdded(Level level, BlockPos pos, RotationNode node) {
        if (level.isClientSide)
            return;
        RotationNetworks.of(level).queueAdded(pos);
    }

    /** Rebuilds every run something was added to since last tick, once per run. */
    static void flushAdded(Level level, Set<BlockPos> added) {
        Set<BlockPos> covered = new HashSet<>();
        for (BlockPos pos : added)
            if (!covered.contains(pos))
                for (RotationNode member : rebuild(level, pos, pos))
                    covered.add(member.getBlockPos());
    }

    public static void onRemoved(Level level, BlockPos pos, RotationNode removed) {
        if (level.isClientSide)
            return;
        if (removed.getNetwork() != null)
            removed.getNetwork().remove(removed);
        // The run may have been cut in two. Rebuild from each side; a neighbour now in a
        // separate component simply flood fills a smaller set. Neighbours still joined to each
        // other share a component, so skip any that the previous rebuild already covered.
        Set<BlockPos> covered = new HashSet<>();
        for (Direction face : Direction.values()) {
            BlockPos neighbourPos = pos.relative(face);
            if (covered.contains(neighbourPos) || nodeAt(level, neighbourPos) == null)
                continue;
            for (RotationNode member : rebuild(level, neighbourPos, null))
                covered.add(member.getBlockPos());
        }
        // A rod to a shaft elsewhere is a neighbour too (RotationNetworks.Link).
        for (RotationNetworks.Link link : List.copyOf(RotationNetworks.of(level).linksAt(pos))) {
            BlockPos far = link.other(pos);
            if (!covered.contains(far) && nodeAt(level, far) != null)
                for (RotationNode member : rebuild(level, far, null))
                    covered.add(member.getBlockPos());
        }
    }

    /** Recompute ratios, target speed and network membership for the whole run at {@code pos}. */
    public static void rebuildFrom(Level level, BlockPos pos) {
        rebuild(level, pos, null);
    }

    /**
     * @param culprit the block just placed, destroyed if it completes a source conflict. Null when
     *                the rebuild was caused by something else (a removal, a clutch toggle): then
     *                the starting node is innocent, so the run jams at zero speed instead.
     */
    private static List<RotationNode> rebuild(Level level, BlockPos pos, @Nullable BlockPos culprit) {
        RotationNode start = nodeAt(level, pos);
        if (start == null)
            return List.of();

        List<RotationNode> component = floodFill(level, start);

        RotationNode strongest = null;
        for (RotationNode node : component)
            if (node.isSource() && (strongest == null
                    || Math.abs(node.getGeneratedRpm().value())
                            > Math.abs(strongest.getGeneratedRpm().value())))
                strongest = node;

        // Ratios are measured against a reference node. The driving source is the natural
        // choice; with nothing driving, any node will do, since the run still has to keep its
        // relative speeds straight while it coasts.
        RotationNode reference = strongest != null ? strongest : start;

        // NaN default so a lookup can tell "never assigned" from "assigned zero" without a
        // second containsKey call -- and so ratios never boxes a Float per node.
        Object2FloatMap<RotationNode> ratios = new Object2FloatOpenHashMap<>();
        ratios.defaultReturnValue(Float.NaN);
        boolean jammed = !assignRatios(level, reference, ratios);
        if (jammed && culprit != null) {
            // Two sources are fighting over the same run. Something has to give, and it is the
            // block that was just placed to complete the conflict.
            level.destroyBlock(culprit, true);
            return List.of();
        }

        // Carry the reference node's actual speed across the rebuild, so adding a shaft to a
        // turning line does not stop it dead. Its ratio is 1 by construction, so its speed is
        // the network's speed.
        // A snapshot, so it has to be a local -- kept typed, per the unit rule.
        Rpm inheritedRpm = reference.getRpm();
        // Read the phase off the old NETWORK, not off the node. A node's own visualAngle is only
        // written when it syncs, so between syncs it is stale, while the network's is live.
        float inheritedPhase = reference.getNetwork() == null ? 0f : reference.getNetwork().getPhase();

        // Retire whatever networks these nodes used to belong to. Without this the old network
        // object stays in the registry with its members still listed, and goes on ticking
        // forever alongside the new one.
        RotationNetworks registry = RotationNetworks.of(level);
        Set<Long> retired = new HashSet<>();
        for (RotationNode node : component) {
            RotationNetwork previous = node.getNetwork();
            if (previous != null && retired.add(previous.id))
                registry.discard(previous.id);
        }

        RotationNetwork network = registry.create();
        for (RotationNode node : component) {
            float r = ratios.getFloat(node);
            node.setRatio(Float.isNaN(r) ? 0f : r);
            node.setNetwork(network);
            network.add(node);
        }
        network.inheritSpeed(inheritedRpm.value());
        network.inheritPhase(inheritedPhase);
        network.setTargetRpm(strongest != null && !jammed ? strongest.getGeneratedRpm() : STOPPED);
        network.recalculate();

        // Tell every client unconditionally. A rebuild often settles on exactly the speed the
        // run was already at, and the tick loop only syncs on a CHANGE -- so without this the
        // client never hears the speed at all, and a perfectly good shaft sits there reporting
        // its RPM while refusing to turn.
        for (RotationNode node : component)
            node.syncSpeedNow();
        return component;
    }

    /**
     * Walk outward from the reference node, giving every reachable node its speed ratio.
     *
     * @return false if two sources demand incompatible speeds of the same node.
     */
    private static boolean assignRatios(Level level, RotationNode reference, Object2FloatMap<RotationNode> ratios) {
        ratios.put(reference, 1f);

        Deque<RotationNode> queue = new ArrayDeque<>();
        queue.add(reference);
        // One buffer, refilled per node rather than allocated per node -- see floodFill for why.
        List<RotationNode> neighbours = new ArrayList<>(6);

        while (!queue.isEmpty()) {
            RotationNode current = queue.poll();
            float currentRatio = ratios.getFloat(current);

            connectedNeighbours(level, current, neighbours);
            for (RotationNode neighbour : neighbours) {
                float gearing = ratioBetween(level, current, neighbour);
                if (gearing == 0)
                    continue;

                float conveyed = currentRatio * gearing;

                float existing = ratios.getFloat(neighbour);
                if (!Float.isNaN(existing)) {
                    // Rejoining the walk at a node we have already set is fine, as long as both
                    // routes agree. Disagreement means the run is geared against itself.
                    if (Math.abs(existing - conveyed) > 1e-4f)
                        return false;
                    continue;
                }

                // A second source in the same run only survives if it happens to want exactly
                // the speed this one is already giving it. Checked in RPM rather than ratio,
                // because a ratio means nothing without a reference speed to apply it to.
                if (neighbour.isSource() && reference.getGeneratedRpm().value() != 0
                        && Math.abs(neighbour.getGeneratedRpm().value() - conveyed * reference.getGeneratedRpm().value()) > 1e-4f)
                    return false;

                ratios.put(neighbour, conveyed);
                queue.add(neighbour);
            }
        }
        return true;
    }

    /** Every rotation node reachable from {@code start} through shaft connections. */
    private static List<RotationNode> floodFill(Level level, RotationNode start) {
        List<RotationNode> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        Deque<RotationNode> queue = new ArrayDeque<>();
        // Reused across every node visited, instead of a fresh ArrayList<>(6) each time --
        // connectedNeighbours only needs its result for the body of this loop, never after.
        List<RotationNode> neighbours = new ArrayList<>(6);

        queue.add(start);
        seen.add(start.getBlockPos());

        while (!queue.isEmpty()) {
            RotationNode current = queue.poll();
            found.add(current);
            connectedNeighbours(level, current, neighbours);
            for (RotationNode neighbour : neighbours)
                if (seen.add(neighbour.getBlockPos()))
                    queue.add(neighbour);
        }
        return found;
    }

    /** Fills {@code out} with every node coupled to {@code node}. Clears it first. */
    private static void connectedNeighbours(Level level, RotationNode node, List<RotationNode> out) {
        out.clear();
        for (Direction face : Direction.values()) {
            RotationNode other = nodeAt(level, node.getBlockPos().relative(face));
            if (other != null && ratioBetween(level, node, other) != 0)
                out.add(other);
        }
        for (RotationNetworks.Link link : RotationNetworks.of(level).linksAt(node.getBlockPos())) {
            RotationNode other = nodeAt(level, link.other(node.getBlockPos()));
            if (other != null && other != node && !out.contains(other))
                out.add(other);
        }
    }

    /**
     * Speed multiplier from {@code from} to {@code to}, or 0 when they are not coupled.
     * <p>
     * Two ways to be coupled, and they are different physical things. Teeth <em>mesh</em>, which
     * reverses direction and changes speed by the ratio of the two gears. A shaft <em>runs
     * through</em>, which is always the same speed -- though not necessarily the same direction,
     * since a gearbox drives its faces off separate bevels. A third way: a <em>link</em>, a rod
     * carrying rotation between two nodes that do not touch (see {@link RotationNetworks.Link}).
     */
    private static float ratioBetween(Level level, RotationNode from, RotationNode to) {
        float linked = RotationNetworks.of(level).linkRatio(from.getBlockPos(), to.getBlockPos());
        if (linked != 0)
            return linked;
        BlockState stateFrom = from.getBlockState();
        BlockState stateTo = to.getBlockState();
        if (!(stateFrom.getBlock() instanceof Rotatable rotatableFrom)
                || !(stateTo.getBlock() instanceof Rotatable rotatableTo))
            return 0;

        BlockPos diff = to.getBlockPos().subtract(from.getBlockPos());
        if (diff.distManhattan(BlockPos.ZERO) != 1)
            return 0;

        Direction face = Direction.getNearest(diff.getX(), diff.getY(), diff.getZ());

        float mesh = rotatableFrom.meshRatioTowards(stateFrom, face, stateTo);
        if (mesh != 0)
            return mesh;

        boolean coupled = rotatableFrom.hasShaftTowards(level, from.getBlockPos(), stateFrom, face)
                && rotatableTo.hasShaftTowards(level, to.getBlockPos(), stateTo, face.getOpposite());
        if (!coupled)
            return 0;

        return rotatableFrom.shaftSignTowards(stateFrom, face)
                * rotatableTo.shaftSignTowards(stateTo, face.getOpposite());
    }

    private static RotationNode nodeAt(Level level, BlockPos pos) {
        if (!level.isLoaded(pos))
            return null;
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof RotationNode node ? node : null;
    }
}
