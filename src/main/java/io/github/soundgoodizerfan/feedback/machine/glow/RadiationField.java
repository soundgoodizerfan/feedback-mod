// Copyright (C) 2026 soundgoodizerfan
// SPDX-License-Identifier: GPL-3.0-or-later
package io.github.soundgoodizerfan.feedback.machine.glow;

import java.util.HashMap;
import java.util.Map;

import io.github.soundgoodizerfan.feedback.core.hazard.Radiation;
import io.github.soundgoodizerfan.feedback.process.MaterialTable;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * The dose at a place, shared: the world is cut into cells of {@link GlowTuning#CELL_BLOCKS} blocks a side, one reading per cell
 * is kept for {@link GlowTuning#CACHE_TICKS}, so a wall of two hundred glass blocks costs the scans of the few cells it spans, not
 * two hundred. Server thread only. A reading is {@link Radiation#rateAt} at the cell's middle block.
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = io.github.soundgoodizerfan.feedback.Feedback.MOD_ID)
public final class RadiationField {

    private record Entry(long tick, float ru) {
    }

    private static final Map<ResourceKey<Level>, Map<Long, Entry>> CACHE = new HashMap<>();

    private RadiationField() {
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onServerStopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        clear();
    }

    /** Forgets every reading: a world that is stopped keeps nothing for the next one. */
    public static void clear() {
        CACHE.clear();
    }

    /** The dose in Ru around {@code pos}, from a cell reading no older than the cache lifetime. */
    public static float at(Level level, BlockPos pos) {
        int n = GlowTuning.CELL_BLOCKS;
        int cx = Math.floorDiv(pos.getX(), n), cy = Math.floorDiv(pos.getY(), n), cz = Math.floorDiv(pos.getZ(), n);
        long key = BlockPos.asLong(cx, cy, cz);
        Map<Long, Entry> cells = CACHE.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        long now = level.getGameTime();
        Entry e = cells.get(key);
        if (e != null && now >= e.tick() && now - e.tick() < GlowTuning.CACHE_TICKS)
            return e.ru();
        if (cells.size() > 512)
            cells.values().removeIf(old -> now - old.tick() > 4 * GlowTuning.CACHE_TICKS);
        float ru = Radiation.rateAt(level, new BlockPos(cx * n + n / 2, cy * n + n / 2, cz * n + n / 2), MaterialTable.get().materials());
        cells.put(key, new Entry(now, ru));
        return ru;
    }
}
