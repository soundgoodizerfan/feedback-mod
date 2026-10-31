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
package io.github.soundgoodizerfan.feedback.core.xp;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Per-level registry of live XP networks, their rebuild and their tick, the {@code FluidNetworks} idea for XP. Runtime
 * only: the nodes are the truth and the networks rebuild from them on load. Any change to a node queues it, and a
 * rebuilt run is awake until it settles.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class XpNetworks {

    private static final Map<LevelAccessor, XpNetworks> PER_LEVEL = new HashMap<>();

    public static XpNetworks of(Level level) {
        return PER_LEVEL.computeIfAbsent(level, l -> new XpNetworks());
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        PER_LEVEL.remove(event.getLevel());
    }

    private Set<BlockPos> dirty = new LinkedHashSet<>();
    private final Set<XpNetwork> stale = new HashSet<>();
    private final Set<XpNetwork> networks = new HashSet<>();

    public void queue(BlockPos pos) {
        dirty.add(pos.immutable());
    }

    public void removed(XpNode node) {
        if (node.network() != null) {
            node.network().members.remove(node);
            stale.add(node.network());
            node.network(null);
        }
        queue(node.entity().getBlockPos());
    }

    /** An unloaded chunk drops its nodes without rebuilding, which could load chunks. */
    public void unloaded(XpNode node) {
        if (node.network() != null) {
            node.network().members.remove(node);
            if (node.network().members.isEmpty())
                networks.remove(node.network());
            node.network(null);
        }
    }

    private void flush(Level level) {
        if (dirty.isEmpty())
            return;
        Set<BlockPos> batch = dirty;
        dirty = new LinkedHashSet<>();

        List<XpNode> seeds = new ArrayList<>();
        for (BlockPos pos : batch) {
            nodeAt(level, pos, seeds);
            for (Direction d : Direction.values())
                nodeAt(level, pos.relative(d), seeds);
        }
        networks.removeAll(stale);
        stale.clear();
        for (XpNode seed : seeds)
            if (seed.network() != null)
                networks.remove(seed.network());

        Set<XpNode> seen = new HashSet<>();
        for (XpNode seed : seeds) {
            if (!seen.add(seed))
                continue;
            XpNetwork net = new XpNetwork();
            ArrayDeque<XpNode> queue = new ArrayDeque<>();
            net.members.add(seed);
            queue.add(seed);
            while (!queue.isEmpty()) {
                XpNode at = queue.poll();
                for (Direction d : Direction.values()) {
                    List<XpNode> next = new ArrayList<>(1);
                    nodeAt(level, at.entity().getBlockPos().relative(d), next);
                    for (XpNode p : next)
                        if (seen.add(p)) {
                            net.members.add(p);
                            queue.add(p);
                        }
                }
                for (XpNode p : at.bridges())
                    if (!p.entity().isRemoved() && seen.add(p)) {
                        net.members.add(p);
                        queue.add(p);
                    }
            }
            for (XpNode m : net.members)
                m.network(net);
            networks.add(net);
        }
    }

    private static void nodeAt(Level level, BlockPos pos, List<XpNode> out) {
        if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof XpNode node && !node.entity().isRemoved()
                && !out.contains(node))
            out.add(node);
    }

    @SubscribeEvent
    public static void onLevelTickStart(LevelTickEvent.Pre event) {
        if (!event.getLevel().isClientSide)
            of(event.getLevel()).flush(event.getLevel());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!event.getLevel().isClientSide)
            for (XpNetwork net : new ArrayList<>(of(event.getLevel()).networks))
                net.tick(event.getLevel());
    }
}
