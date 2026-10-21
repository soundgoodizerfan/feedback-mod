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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

import io.github.soundgoodizerfan.feedback.Feedback;

/**
 * Per-level registry of live rotation networks.
 * <p>
 * Networks are runtime-only and nothing here is saved. On load every node re-announces itself
 * and the networks rebuild from the blocks, which are the real source of truth. Momentum is the
 * one thing lost across a reload, which is a reasonable thing to lose when the chunk was not
 * ticking anyway.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public class RotationNetworks {

    // Not a WeakHashMap: the value reaches the key through its nodes, so it would never clear.
    private static final Map<LevelAccessor, RotationNetworks> PER_LEVEL = new HashMap<>();

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        PER_LEVEL.remove(event.getLevel());
    }

    /**
     * A joint between two nodes that do not touch -- a rod of the free-form drivetrain carrying
     * rotation from one shaft to another. {@code ratio} is the speed of {@code b} over {@code a}
     * (a rod is a rod: plus or minus one); it reads the same both ways.
     */
    public record Link(BlockPos a, BlockPos b, float ratio) {
        BlockPos other(BlockPos pos) {
            return a.equals(pos) ? b : a;
        }
    }

    private final Map<Long, RotationNetwork> networks = new HashMap<>();
    /** Links by whoever published them (a frame's block), so one owner's set is replaced whole. */
    private final Map<BlockPos, Set<Link>> owned = new HashMap<>();
    private final Map<BlockPos, List<Link>> byEnd = new HashMap<>();
    /** Nodes loaded or placed since the last tick -- see {@link RotationPropagator#onAdded}. */
    private Set<BlockPos> added = new LinkedHashSet<>();
    private long nextId;

    public static RotationNetworks of(Level level) {
        return PER_LEVEL.computeIfAbsent(level, l -> new RotationNetworks());
    }

    void queueAdded(BlockPos pos) {
        added.add(pos.immutable());
    }

    /** Called at the start of each level tick, before anything reads a network. */
    void flushAdded(Level level) {
        if (added.isEmpty())
            return;
        Set<BlockPos> batch = added;
        added = new LinkedHashSet<>();
        RotationPropagator.flushAdded(level, batch);
    }

    /**
     * Replaces everything {@code owner} links. Returns the nodes the change touches, old ends and
     * new, for the caller to rebuild; empty when nothing changed.
     */
    public Set<BlockPos> setLinks(BlockPos owner, List<Link> links) {
        Set<Link> next = Set.copyOf(links);
        Set<Link> old = owned.getOrDefault(owner, Set.of());
        if (old.equals(next))
            return Set.of();
        if (next.isEmpty())
            owned.remove(owner);
        else
            owned.put(owner, next);
        byEnd.clear();
        for (Set<Link> set : owned.values())
            for (Link link : set) {
                byEnd.computeIfAbsent(link.a(), k -> new ArrayList<>()).add(link);
                byEnd.computeIfAbsent(link.b(), k -> new ArrayList<>()).add(link);
            }
        Set<BlockPos> touched = new LinkedHashSet<>();
        for (Link link : old) {
            touched.add(link.a());
            touched.add(link.b());
        }
        for (Link link : next) {
            touched.add(link.a());
            touched.add(link.b());
        }
        return touched;
    }

    List<Link> linksAt(BlockPos pos) {
        return byEnd.isEmpty() ? List.of() : byEnd.getOrDefault(pos, List.of());
    }

    /** Speed of {@code to} over {@code from} across a link, or 0 when they are not linked. */
    float linkRatio(BlockPos from, BlockPos to) {
        for (Link link : linksAt(from))
            if (link.other(from).equals(to))
                return link.ratio();
        return 0;
    }

    public RotationNetwork create() {
        RotationNetwork network = new RotationNetwork(nextId++);
        networks.put(network.id, network);
        return network;
    }

    public void discard(long id) {
        networks.remove(id);
    }

    /** Advance every network one tick. Copied first, because a network can discard itself. */
    public void tickAll() {
        if (networks.isEmpty())
            return;
        List<RotationNetwork> snapshot = new ArrayList<>(networks.values());
        for (RotationNetwork network : snapshot)
            network.tick();
    }
}
