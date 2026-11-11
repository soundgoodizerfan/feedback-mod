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
import io.github.soundgoodizerfan.feedback.machine.adapter.RotationAdapterBlock;
import io.github.soundgoodizerfan.feedback.vehicle.micro.MicroCellBlockEntity;
import io.github.soundgoodizerfan.feedback.vehicle.micro.MicroRotor;

import org.jetbrains.annotations.Nullable;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
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
import net.minecraft.core.Vec3i;
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
 *
 * <h2>Members smaller than a block</h2>
 * A member is a {@link RotationMember}: a full-size {@link RotationNode}, or a {@link MicroRotor},
 * one rotation voxel in a micro cell. Neighbours are found on the quarter grid between rotors, and
 * across a cell face between a Rotation Adapter's micro face and the central rotors of that face --
 * the only full-size block that links to a cell -- see
 * {@code docs/feedback_micro_spec.md}, <em>Micro rotation nodes</em>. A rod links full-size nodes only.
 * <p>
 * Design derived from Create's {@code RotationPropagator} (MIT) -- see THIRD-PARTY-LICENSES.md.
 */
public class RotationPropagator {

    /** A run with no source is not turning. */
    private static final Rpm STOPPED = new Rpm(0);

    private static final int N = MicroCellBlockEntity.N;

    /** The twelve edge diagonals: one step along each of two axes. Where cogs mesh off the faces. */
    private static final List<Vec3i> EDGE_DIAGONALS = edgeDiagonals();

    private static List<Vec3i> edgeDiagonals() {
        List<Vec3i> out = new ArrayList<>(12);
        for (int x = -1; x <= 1; x++)
            for (int y = -1; y <= 1; y++)
                for (int z = -1; z <= 1; z++)
                    if (Math.abs(x) + Math.abs(y) + Math.abs(z) == 2)
                        out.add(new Vec3i(x, y, z));
        return out;
    }

    /**
     * Queued, not rebuilt at once: a chunk loading a thousand-shaft run calls this a thousand
     * times, and rebuilding each one floods the whole run every time -- O(n²) on world load.
     * {@link #flushAdded} rebuilds each run once at the start of the next tick.
     */
    public static void onAdded(Level level, BlockPos pos, RotationNode node) {
        queue(level, pos);
    }

    /** Queue every member at {@code pos} for next tick's rebuild: a node, or all of a micro cell's rotors. */
    public static void queue(Level level, BlockPos pos) {
        if (!level.isClientSide)
            RotationNetworks.of(level).queueAdded(pos);
    }

    /** Rebuilds every run something was added to since last tick, once per run. */
    static void flushAdded(Level level, Set<BlockPos> added) {
        Set<RotationMember> covered = new HashSet<>();
        for (BlockPos pos : added)
            for (RotationMember start : membersAt(level, pos))
                if (!covered.contains(start))
                    // Only a full-size node is destroyed for completing a source conflict: a rotor's
                    // block is its whole cell, every voxel in it, so a rotor's run jams instead.
                    covered.addAll(rebuild(level, start, start instanceof RotationNode ? pos : null));
    }

    public static void onRemoved(Level level, BlockPos pos, RotationNode removed) {
        if (level.isClientSide)
            return;
        if (removed.getNetwork() != null)
            removed.getNetwork().remove(removed);
        // The run may have been cut in two. Rebuild from each side; a neighbour now in a
        // separate component simply flood fills a smaller set. Neighbours still joined to each
        // other share a component, so skip any that the previous rebuild already covered.
        Set<RotationMember> covered = new HashSet<>();
        for (Direction face : Direction.values())
            rebuildUncovered(level, membersAt(level, pos.relative(face)), covered);
        // A rod to a shaft elsewhere is a neighbour too (RotationNetworks.Link).
        for (RotationNetworks.Link link : List.copyOf(RotationNetworks.of(level).linksAt(pos)))
            rebuildUncovered(level, membersAt(level, link.other(pos)), covered);
    }

    /**
     * Rotors left their cell (a voxel taken out, the cell broken). Each leaves its network, and the
     * rest of that network is rebuilt from wherever it still stands, since the cut may have split it.
     */
    public static void onRemoved(Level level, List<? extends RotationMember> gone) {
        if (level.isClientSide || gone.isEmpty())
            return;
        List<RotationMember> rest = new ArrayList<>();
        for (RotationMember member : gone) {
            RotationNetwork network = member.getNetwork();
            if (network == null)
                continue;
            rest.addAll(network.members());
            network.remove(member);
            member.setNetwork(null);
        }
        rest.removeAll(gone);
        rebuildUncovered(level, rest, new HashSet<>());
    }

    private static void rebuildUncovered(Level level, List<? extends RotationMember> starts, Set<RotationMember> covered) {
        for (RotationMember start : starts)
            if (!covered.contains(start) && start.isLive())
                covered.addAll(rebuild(level, start, null));
    }

    /** Recompute ratios, target speed and network membership for the whole run at {@code pos}. */
    public static void rebuildFrom(Level level, BlockPos pos) {
        rebuildUncovered(level, membersAt(level, pos), new HashSet<>());
    }

    /**
     * @param culprit the block just placed, destroyed if it completes a source conflict. Null when
     *                the rebuild was caused by something else (a removal, a clutch toggle): then
     *                the starting node is innocent, so the run jams at zero speed instead.
     */
    private static List<RotationMember> rebuild(Level level, RotationMember start, @Nullable BlockPos culprit) {
        List<RotationMember> component = floodFill(level, start);

        RotationMember strongest = null;
        for (RotationMember node : component)
            if (node.isSource() && (strongest == null
                    || Math.abs(node.getGeneratedRpm().value())
                            > Math.abs(strongest.getGeneratedRpm().value())))
                strongest = node;

        // Ratios are measured against a reference node. The driving source is the natural
        // choice; with nothing driving, any node will do, since the run still has to keep its
        // relative speeds straight while it coasts.
        RotationMember reference = strongest != null ? strongest : start;

        // NaN default so a lookup can tell "never assigned" from "assigned zero" without a
        // second containsKey call -- and so ratios never boxes a Float per node.
        Object2FloatMap<RotationMember> ratios = new Object2FloatOpenHashMap<>();
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
        for (RotationMember node : component) {
            RotationNetwork previous = node.getNetwork();
            if (previous != null && retired.add(previous.id))
                registry.discard(previous.id);
        }

        RotationNetwork network = registry.create();
        for (RotationMember node : component) {
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
        for (RotationMember node : component)
            node.syncSpeedNow();
        return component;
    }

    /**
     * Walk outward from the reference node, giving every reachable node its speed ratio.
     *
     * @return false if two sources demand incompatible speeds of the same node.
     */
    private static boolean assignRatios(Level level, RotationMember reference, Object2FloatMap<RotationMember> ratios) {
        ratios.put(reference, 1f);

        Deque<RotationMember> queue = new ArrayDeque<>();
        queue.add(reference);
        // One buffer, refilled per node rather than allocated per node -- see floodFill for why.
        List<RotationMember> neighbours = new ArrayList<>(6);
        FloatArrayList gearings = new FloatArrayList(6);

        while (!queue.isEmpty()) {
            RotationMember current = queue.poll();
            float currentRatio = ratios.getFloat(current);

            connectedNeighbours(level, current, neighbours, gearings);
            for (int i = 0; i < neighbours.size(); i++) {
                RotationMember neighbour = neighbours.get(i);
                float conveyed = currentRatio * gearings.getFloat(i);

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

    /** Every rotation member reachable from {@code start} through shaft connections. */
    private static List<RotationMember> floodFill(Level level, RotationMember start) {
        List<RotationMember> found = new ArrayList<>();
        // By identity, not position: a cell's rotors share one block position.
        Set<RotationMember> seen = new HashSet<>();
        Deque<RotationMember> queue = new ArrayDeque<>();
        // Reused across every node visited, instead of a fresh ArrayList<>(6) each time --
        // connectedNeighbours only needs its result for the body of this loop, never after.
        List<RotationMember> neighbours = new ArrayList<>(6);
        FloatArrayList gearings = new FloatArrayList(6);

        queue.add(start);
        seen.add(start);

        while (!queue.isEmpty()) {
            RotationMember current = queue.poll();
            found.add(current);
            connectedNeighbours(level, current, neighbours, gearings);
            for (RotationMember neighbour : neighbours)
                if (seen.add(neighbour))
                    queue.add(neighbour);
        }
        return found;
    }

    /** Every rotation member standing in block {@code pos}: a node, a micro cell's rotors, or none. */
    public static List<? extends RotationMember> membersAt(Level level, BlockPos pos) {
        if (!level.isLoaded(pos))
            return List.of();
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RotationNode node)
            return List.of(node);
        if (be instanceof MicroCellBlockEntity cell)
            return cell.rotors();
        return List.of();
    }

    /**
     * Fills {@code out} with every member coupled to {@code member}, and {@code gearings} with the
     * speed multiplier to each, index for index. Clears both first.
     */
    private static void connectedNeighbours(Level level, RotationMember member, List<RotationMember> out, FloatArrayList gearings) {
        out.clear();
        gearings.clear();
        if (member instanceof MicroRotor rotor) {
            rotorNeighbours(level, rotor, out, gearings);
            return;
        }
        if (!(member instanceof RotationNode node))
            return;
        BlockPos pos = node.getBlockPos();
        RotationNetworks networks = RotationNetworks.of(level);
        for (Direction face : Direction.values()) {
            BlockPos next = pos.relative(face);
            if (!level.isLoaded(next))
                continue;
            BlockEntity be = level.getBlockEntity(next);
            if (be instanceof RotationNode other) {
                float linked = networks.linkRatio(pos, next);
                add(out, gearings, other, linked != 0 ? linked
                        : ratioBetween(level, pos, node.getBlockState(), next, other.getBlockState(), face));
            } else if (be instanceof MicroCellBlockEntity cell && RotationAdapterBlock.microFace(node.getBlockState()) == face) {
                // The mixed link, an adapter's micro face: the central rotors of the cell's face toward it.
                for (int i : centralFace(face.getOpposite())) {
                    MicroRotor other = cell.rotor(i);
                    if (other != null)
                        add(out, gearings, other, microLink(level, next, other.state(), face.getOpposite()));
                }
            }
        }
        BlockState state = node.getBlockState();
        if (state.getBlock() instanceof Rotatable rotatable && rotatable.meshesDiagonally(state))
            for (Vec3i offset : EDGE_DIAGONALS) {
                BlockPos at = pos.offset(offset);
                // Full-size teeth mesh full-size teeth only: a cell's rotors are never found here.
                if (level.isLoaded(at) && level.getBlockEntity(at) instanceof RotationNode other)
                    add(out, gearings, other, rotatable.meshRatioAt(state, offset, other.getBlockState()));
            }
        for (RotationNetworks.Link link : networks.linksAt(pos)) {
            BlockPos far = link.other(pos);
            if (level.isLoaded(far) && level.getBlockEntity(far) instanceof RotationNode other
                    && other != node && !out.contains(other))
                add(out, gearings, other, networks.linkRatio(pos, far));
        }
    }

    /**
     * A rotor's neighbours: the voxel one step each way on the quarter grid, in its own cell or the
     * next; and, from a central voxel of a cell face, a Rotation Adapter whose micro face meets it.
     */
    private static void rotorNeighbours(Level level, MicroRotor rotor, List<RotationMember> out, FloatArrayList gearings) {
        BlockPos pos = rotor.getBlockPos();
        int x = rotor.index() & 3, y = rotor.index() >> 2 & 3, z = rotor.index() >> 4;
        BlockState state = rotor.state();
        if (state.getBlock() instanceof Rotatable rotatable && rotatable.meshesDiagonally(state))
            for (Vec3i offset : EDGE_DIAGONALS) {
                MicroRotor other = rotorAt(level, rotor, x + offset.getX(), y + offset.getY(), z + offset.getZ());
                if (other != null)
                    add(out, gearings, other, rotatable.meshRatioAt(state, offset, other.state()));
            }
        for (Direction face : Direction.values()) {
            int nx = x + face.getStepX(), ny = y + face.getStepY(), nz = z + face.getStepZ();
            boolean inside = nx >= 0 && ny >= 0 && nz >= 0 && nx < N && ny < N && nz < N;
            BlockPos next = inside ? pos : pos.relative(face);
            if (!inside && !level.isLoaded(next))
                continue;
            BlockEntity be = inside ? rotor.cell() : level.getBlockEntity(next);
            if (be instanceof MicroCellBlockEntity cell) {
                MicroRotor other = cell.rotor(MicroCellBlockEntity.index(Math.floorMod(nx, N), Math.floorMod(ny, N), Math.floorMod(nz, N)));
                if (other != null)
                    add(out, gearings, other, ratioBetween(level, pos, rotor.state(), next, other.state(), face));
            } else if (be instanceof RotationNode node && isCentralFace(rotor.index(), face)
                    && RotationAdapterBlock.microFace(node.getBlockState()) == face.getOpposite()) {
                add(out, gearings, node, microLink(level, pos, rotor.state(), face));
            }
        }
    }

    /**
     * The rotor at voxel ({@code x}, {@code y}, {@code z}) counted from {@code rotor}'s cell corner,
     * in that cell or any cell round it; null if none or not loaded.
     */
    @Nullable
    private static MicroRotor rotorAt(Level level, MicroRotor rotor, int x, int y, int z) {
        BlockPos at = rotor.getBlockPos().offset(Math.floorDiv(x, N), Math.floorDiv(y, N), Math.floorDiv(z, N));
        BlockEntity be = at.equals(rotor.getBlockPos()) ? rotor.cell() : level.isLoaded(at) ? level.getBlockEntity(at) : null;
        return be instanceof MicroCellBlockEntity cell
                ? cell.rotor(MicroCellBlockEntity.index(Math.floorMod(x, N), Math.floorMod(y, N), Math.floorMod(z, N)))
                : null;
    }

    private static void add(List<RotationMember> out, FloatArrayList gearings, RotationMember other, float gearing) {
        if (gearing != 0) {
            out.add(other);
            gearings.add(gearing);
        }
    }

    /**
     * The four voxels in the middle of a cell's {@code face}: the ones a full-size shaft (six to ten
     * sixteenths across) overlaps, and so the only ones on its line.
     */
    private static int[] centralFace(Direction face) {
        int layer = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? N - 1 : 0;
        int[] out = new int[4];
        int k = 0;
        for (int a = 1; a <= 2; a++)
            for (int b = 1; b <= 2; b++)
                out[k++] = switch (face.getAxis()) {
                    case X -> MicroCellBlockEntity.index(layer, a, b);
                    case Y -> MicroCellBlockEntity.index(a, layer, b);
                    case Z -> MicroCellBlockEntity.index(a, b, layer);
                };
        return out;
    }

    private static boolean isCentralFace(int index, Direction face) {
        for (int i : centralFace(face))
            if (i == index)
                return true;
        return false;
    }

    /**
     * Speed multiplier from a block of {@code from} at {@code fromPos} to one of {@code to} across
     * {@code face}, or 0 when they are not coupled. Run on block states, so a voxel couples by
     * exactly the rule its full-size block does.
     * <p>
     * Two ways to be coupled, and they are different physical things. Teeth <em>mesh</em>, which
     * reverses direction and changes speed by the ratio of the two gears. A shaft <em>runs
     * through</em>, which is always the same speed -- though not necessarily the same direction,
     * since a gearbox drives its faces off separate bevels. A third way, a <em>link</em> (a rod
     * between two nodes that do not touch, {@link RotationNetworks.Link}), is the caller's.
     */
    private static float ratioBetween(Level level, BlockPos fromPos, BlockState from, BlockPos toPos, BlockState to, Direction face) {
        if (!(from.getBlock() instanceof Rotatable rotatableFrom))
            return 0;
        float mesh = rotatableFrom.meshRatioTowards(from, face, to);
        if (mesh != 0)
            return mesh;
        return shaftBetween(level, fromPos, from, toPos, to, face);
    }

    /**
     * A voxel of {@code state} in the cell at {@code pos} against an adapter's micro face across
     * {@code face}: coupled if the voxel presents a shaft that way, at the same speed with the
     * voxel's sign (the adapter's own is +1). Never meshing: a quarter cog and a full one are not the
     * same pitch. The micro spec says why the boundary is not a gear ratio.
     */
    private static float microLink(Level level, BlockPos pos, BlockState state, Direction face) {
        if (!(state.getBlock() instanceof Rotatable rotatable) || !rotatable.hasShaftTowards(level, pos, state, face))
            return 0;
        return rotatable.shaftSignTowards(state, face);
    }

    /** The shaft coupling alone, without meshing. Same speed, each side's sign. */
    private static float shaftBetween(Level level, BlockPos fromPos, BlockState from, BlockPos toPos, BlockState to, Direction face) {
        if (!(from.getBlock() instanceof Rotatable rotatableFrom) || !(to.getBlock() instanceof Rotatable rotatableTo))
            return 0;
        if (!rotatableFrom.hasShaftTowards(level, fromPos, from, face)
                || !rotatableTo.hasShaftTowards(level, toPos, to, face.getOpposite()))
            return 0;
        return rotatableFrom.shaftSignTowards(from, face) * rotatableTo.shaftSignTowards(to, face.getOpposite());
    }
}
