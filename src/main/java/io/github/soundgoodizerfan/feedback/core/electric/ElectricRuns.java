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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.soundgoodizerfan.feedback.Feedback;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Per-level registry of live electrical runs, and the level-tick hooks that advance them.
 *
 * <p>Runs are runtime-only and nothing here is saved: on load every node re-announces itself and
 * the runs rebuild from the blocks, which are the source of truth -- the same as
 * {@code RotationNetworks}. The tick hooks live here rather than in a second class because a run
 * has no state of its own across a reload worth separating them for.
 */
@EventBusSubscriber(modid = Feedback.MOD_ID)
public final class ElectricRuns {

    // Not a WeakHashMap: the value reaches the key through its nodes, so it would never clear.
    private static final Map<LevelAccessor, ElectricRuns> PER_LEVEL = new HashMap<>();

    private final Set<ElectricRun> runs = new LinkedHashSet<>();
    private Set<BlockPos> added = new LinkedHashSet<>();

    public static ElectricRuns of(Level level) {
        return PER_LEVEL.computeIfAbsent(level, l -> new ElectricRuns());
    }

    void queueAdded(BlockPos pos) {
        added.add(pos.immutable());
    }

    void add(ElectricRun run) {
        runs.add(run);
    }

    void discard(ElectricRun run) {
        runs.remove(run);
    }

    /** How many runs this level holds. For tests. */
    public int count() {
        return runs.size();
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        PER_LEVEL.remove(event.getLevel());
    }

    /** Nodes loaded or placed last tick join their runs before anything ticks. */
    @SubscribeEvent
    public static void onLevelTickStart(LevelTickEvent.Pre event) {
        if (event.getLevel().isClientSide)
            return;
        ElectricRuns registry = of(event.getLevel());
        if (registry.added.isEmpty())
            return;
        Set<BlockPos> batch = registry.added;
        registry.added = new LinkedHashSet<>();
        ElectricPropagator.flushAdded(event.getLevel(), batch);
    }

    /** Advance every run one tick. Copied first, because a wire destroying itself rebuilds runs. */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide)
            return;
        ElectricRuns registry = of(event.getLevel());
        if (registry.runs.isEmpty())
            return;
        List<ElectricRun> snapshot = new ArrayList<>(registry.runs);
        for (ElectricRun run : snapshot)
            if (registry.runs.contains(run))
                run.tick();
    }
}
