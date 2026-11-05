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
package io.github.soundgoodizerfan.feedback.core.tube;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.Feedback;
import io.github.soundgoodizerfan.feedback.core.thermal.FluidHeat;
import io.github.soundgoodizerfan.feedback.core.thermal.HeatedTank;
import io.github.soundgoodizerfan.feedback.machine.tube.TubeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Per-level registry of live tube runs, their rebuild and their tick: {@code FluidNetworks} for tubes.
 * Runtime only. The air of a run is lost across a rebuild; capsules are not, because they sit in the
 * tube blocks and the new run schedules every one it finds.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public class TubeNetworks {

    private static final Map<LevelAccessor, TubeNetworks> PER_LEVEL = new HashMap<>();

    public static TubeNetworks of(Level level) {
        return PER_LEVEL.computeIfAbsent(level, l -> new TubeNetworks());
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        PER_LEVEL.remove(event.getLevel());
    }

    private Set<BlockPos> dirty = new LinkedHashSet<>();
    private final Set<TubeNetwork> stale = new HashSet<>();
    private final Set<TubeNetwork> networks = new HashSet<>();

    public void queue(BlockPos pos) {
        dirty.add(pos.immutable());
    }

    public void removed(TubeBlockEntity tube) {
        if (tube.network() != null) {
            tube.network().members.remove(tube);
            stale.add(tube.network());
            tube.setNetwork(null);
        }
        queue(tube.getBlockPos());
    }

    /** An unloaded chunk drops its tubes without a rebuild, which could load chunks. */
    public void unloaded(TubeBlockEntity tube) {
        TubeNetwork net = tube.network();
        if (net != null) {
            net.members.remove(tube);
            if (net.members.isEmpty())
                networks.remove(net);
            tube.setNetwork(null);
        }
    }

    private void flush(Level level) {
        if (dirty.isEmpty())
            return;
        Set<BlockPos> batch = dirty;
        dirty = new LinkedHashSet<>();

        List<TubeBlockEntity> seeds = new ArrayList<>();
        for (BlockPos pos : batch) {
            tubeAt(level, pos, seeds);
            for (Direction d : Direction.values())
                tubeAt(level, pos.relative(d), seeds);
        }

        Set<TubeNetwork> old = new HashSet<>(stale);
        stale.clear();
        for (TubeBlockEntity seed : seeds)
            if (seed.network() != null)
                old.add(seed.network());

        HeatedTank pool = new HeatedTank(Integer.MAX_VALUE / 2, () -> { });
        for (TubeNetwork net : old) {
            networks.remove(net);
            pool.fill(FluidHeat.stamp(net.ledger.getFluid(), net.ledger.temperature().value()),
                    IFluidHandler.FluidAction.EXECUTE);
        }

        List<Set<TubeBlockEntity>> parts = new ArrayList<>();
        Set<TubeBlockEntity> seen = new HashSet<>();
        int total = 0;
        for (TubeBlockEntity seed : seeds) {
            if (!seen.add(seed))
                continue;
            Set<TubeBlockEntity> part = new HashSet<>();
            ArrayDeque<TubeBlockEntity> queue = new ArrayDeque<>();
            part.add(seed);
            queue.add(seed);
            while (!queue.isEmpty()) {
                TubeBlockEntity at = queue.poll();
                for (Direction d : Direction.values()) {
                    List<TubeBlockEntity> next = new ArrayList<>(1);
                    tubeAt(level, at.getBlockPos().relative(d), next);
                    for (TubeBlockEntity t : next)
                        if (seen.add(t)) {
                            part.add(t);
                            queue.add(t);
                        }
                }
            }
            parts.add(part);
            total += part.size();
        }

        int poolMb = pool.getFluidAmount();
        for (Set<TubeBlockEntity> part : parts) {
            TubeNetwork net = new TubeNetwork(part.size());
            net.members.addAll(part);
            for (TubeBlockEntity t : part) {
                t.setNetwork(net);
                net.schedule(t);
            }
            networks.add(net);
            int share = (int) ((long) poolMb * part.size() / total);
            if (share > 0)
                net.ledger.fill(pool.drain(share, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private static void tubeAt(Level level, BlockPos pos, List<TubeBlockEntity> out) {
        if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof TubeBlockEntity t && !t.isRemoved() && !out.contains(t))
            out.add(t);
    }

    @SubscribeEvent
    public static void onLevelTickStart(LevelTickEvent.Pre event) {
        if (!event.getLevel().isClientSide)
            of(event.getLevel()).flush(event.getLevel());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide)
            return;
        TubeNetworks reg = of(event.getLevel());
        for (TubeNetwork net : new ArrayList<>(reg.networks))
            net.tick(event.getLevel());
    }
}
