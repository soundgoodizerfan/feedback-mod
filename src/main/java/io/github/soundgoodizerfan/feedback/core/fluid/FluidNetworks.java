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
package io.github.soundgoodizerfan.feedback.core.fluid;

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
import io.github.soundgoodizerfan.feedback.machine.pipe.PipeBlockEntity;

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
 * Per-level registry of live fluid networks, their rebuild, and their tick.
 * <p>
 * Runtime only, like {@code RotationNetworks}: the pipes are the truth and the networks rebuild
 * from them on load. Unlike momentum, the fluid in a run is lost across a reload -- a pipe is
 * transit, and the tanks at either end keep theirs.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public class FluidNetworks {

    private static final Map<LevelAccessor, FluidNetworks> PER_LEVEL = new HashMap<>();

    public static FluidNetworks of(Level level) {
        return PER_LEVEL.computeIfAbsent(level, l -> new FluidNetworks());
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        PER_LEVEL.remove(event.getLevel());
    }

    /** Pipes placed, loaded or removed since the last tick; they are rebuilt together before anything ticks. */
    private Set<BlockPos> dirty = new LinkedHashSet<>();
    /** Networks that lost a member and must be dissolved. */
    private final Set<FluidNetwork> stale = new HashSet<>();
    private final Set<FluidNetwork> networks = new HashSet<>();

    public void queue(BlockPos pos) {
        dirty.add(pos.immutable());
    }

    public void removed(PipeBlockEntity pipe) {
        if (pipe.network != null) {
            pipe.network.members.remove(pipe);
            stale.add(pipe.network);
            pipe.network = null;
        }
        queue(pipe.getBlockPos());
    }

    /** An unloaded chunk drops its pipes without rebuilding, which could load chunks. */
    public void unloaded(PipeBlockEntity pipe) {
        if (pipe.network != null) {
            pipe.network.members.remove(pipe);
            if (pipe.network.members.isEmpty())
                networks.remove(pipe.network);
            pipe.network = null;
        }
    }

    private void flush(Level level) {
        if (dirty.isEmpty())
            return;
        Set<BlockPos> batch = dirty;
        dirty = new LinkedHashSet<>();

        List<PipeBlockEntity> seeds = new ArrayList<>();
        for (BlockPos pos : batch) {
            pipeAt(level, pos, seeds);
            for (Direction d : Direction.values())
                pipeAt(level, pos.relative(d), seeds);
        }

        Set<FluidNetwork> old = new HashSet<>(stale);
        stale.clear();
        for (PipeBlockEntity seed : seeds)
            if (seed.network != null)
                old.add(seed.network);

        // shortcut: two different fluids joined by one new pipe keep the larger and lose the other,
        // where the spec lets them mix and react (5.2); revisit with the reaction table.
        HeatedTank pool = new HeatedTank(Integer.MAX_VALUE / 2, () -> { });
        for (FluidNetwork net : old) {
            networks.remove(net);
            pool.fill(FluidHeat.stamp(net.ledger.getFluid(), net.ledger.temperature().value()),
                    IFluidHandler.FluidAction.EXECUTE);
        }

        List<Set<PipeBlockEntity>> parts = new ArrayList<>();
        Set<PipeBlockEntity> seen = new HashSet<>();
        int total = 0;
        for (PipeBlockEntity seed : seeds) {
            if (!seen.add(seed))
                continue;
            Set<PipeBlockEntity> part = new HashSet<>();
            ArrayDeque<PipeBlockEntity> queue = new ArrayDeque<>();
            part.add(seed);
            queue.add(seed);
            while (!queue.isEmpty()) {
                PipeBlockEntity at = queue.poll();
                for (Direction d : Direction.values()) {
                    List<PipeBlockEntity> next = new ArrayList<>(1);
                    pipeAt(level, at.getBlockPos().relative(d), next);
                    for (PipeBlockEntity p : next)
                        if (seen.add(p)) {
                            part.add(p);
                            queue.add(p);
                        }
                }
            }
            parts.add(part);
            total += part.size();
        }

        int poolMb = pool.getFluidAmount();
        for (Set<PipeBlockEntity> part : parts) {
            FluidNetwork net = new FluidNetwork(part.size());
            net.members.addAll(part);
            for (PipeBlockEntity p : part)
                p.network = net;
            networks.add(net);
            int share = (int) ((long) poolMb * part.size() / total);
            if (share > 0)
                net.ledger.fill(pool.drain(share, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private static void pipeAt(Level level, BlockPos pos, List<PipeBlockEntity> out) {
        if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof PipeBlockEntity pipe && !pipe.isRemoved()
                && !out.contains(pipe))
            out.add(pipe);
    }

    private void tickAll(Level level) {
        for (FluidNetwork net : new ArrayList<>(networks))
            net.tick(level);
    }

    @SubscribeEvent
    public static void onLevelTickStart(LevelTickEvent.Pre event) {
        if (!event.getLevel().isClientSide)
            of(event.getLevel()).flush(event.getLevel());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!event.getLevel().isClientSide)
            of(event.getLevel()).tickAll(event.getLevel());
    }
}
